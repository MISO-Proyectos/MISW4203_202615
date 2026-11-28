package co.edu.uniandes.misw4203.vinilos.ui.albums.tracks

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import co.edu.uniandes.misw4203.vinilos.R
import co.edu.uniandes.misw4203.vinilos.data.model.Album
import co.edu.uniandes.misw4203.vinilos.databinding.FragmentAddTrackBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

/**
 * Pantalla "Agregar track" (HU08). Le pasa al ViewModel lo que escribe el usuario,
 * dibuja la tarjeta del álbum y los errores y, cuando la canción se guarda, vuelve al
 * detalle dejando el resultado en [RESULT_ADDED_TRACK] para que el detalle lo muestre.
 */
class AddTrackFragment : Fragment() {

    private var _binding: FragmentAddTrackBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddTrackViewModel by viewModels { AddTrackViewModel.Factory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTrackBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.trackNameInput.doAfterTextChanged {
            viewModel.onNameChanged(it?.toString().orEmpty())
        }
        binding.trackDurationInput.doAfterTextChanged {
            viewModel.onDurationChanged(it?.toString().orEmpty())
        }
        binding.trackDurationInput.setOnEditorActionListener { _, _, _ ->
            save()
            true
        }
        binding.saveTrackButton.setOnClickListener { save() }
        binding.cancelButton.setOnClickListener { findNavController().navigateUp() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.events.collect(::handleEvent) }
            }
        }
    }

    private fun save() {
        hideKeyboard()
        viewModel.save()
    }

    private fun render(state: AddTrackUiState) {
        val bannerWasVisible = binding.errorBanner.isVisible
        binding.errorBanner.isVisible = state.showErrorBanner
        // El aviso está arriba: si recién aparece, se sube para que el usuario lo vea
        if (state.showErrorBanner && !bannerWasVisible) binding.addTrackScroll.smoothScrollTo(0, 0)

        binding.albumSummary.isVisible = state.album != null
        state.album?.let(::showAlbum)

        showError(binding.trackNameLayout, state.errors[TrackFormField.NAME])
        showError(binding.trackDurationLayout, state.errors[TrackFormField.DURATION])

        // Los campos siguen activos mientras guarda: si se desactivan, TalkBack anuncia cada uno
        // como "inhabilitado" y esos avisos tapan el de éxito. Solo se bloquean los botones;
        // el ViewModel ignora save() mientras guarda y envía los valores del momento del toque.
        val enabled = !state.isSaving
        binding.saveTrackButton.isEnabled = enabled
        binding.cancelButton.isEnabled = enabled
        binding.savingIndicator.isVisible = state.isSaving
    }

    private fun showAlbum(album: Album) {
        binding.albumName.text = album.name
        val tracks = resources.getQuantityString(
            R.plurals.add_track_album_tracks, album.tracks.size, album.tracks.size
        )
        val performers = album.performerNames.ifBlank { getString(R.string.album_unknown_performer) }
        binding.albumInfo.text = getString(R.string.add_track_album_info, performers, tracks)
        Glide.with(binding.albumCover)
            .load(album.cover.ifBlank { null })
            .placeholder(R.drawable.ic_album)
            .error(R.drawable.ic_album)
            .override(resources.getDimensionPixelSize(R.dimen.add_track_cover_size))
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .into(binding.albumCover)
    }

    private fun showError(layout: TextInputLayout, error: TrackFormError?) {
        val message = error?.let { getString(it.messageRes) }
        if (layout.error?.toString() != message) layout.error = message
    }

    private fun handleEvent(event: AddTrackEvent) {
        when (event) {
            is AddTrackEvent.Added -> {
                val navController = findNavController()
                navController.previousBackStackEntry?.savedStateHandle?.set(
                    RESULT_ADDED_TRACK,
                    bundleOf(RESULT_TRACK_ID to event.trackId, RESULT_TRACK_NAME to event.trackName)
                )
                navController.popBackStack()
            }
            is AddTrackEvent.Failed ->
                Snackbar.make(
                    binding.root,
                    event.message.ifBlank { getString(R.string.add_track_error_default) },
                    Snackbar.LENGTH_LONG
                ).show()
        }
    }

    private fun hideKeyboard() {
        val focused = activity?.currentFocus ?: return
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(focused.windowToken, 0)
        focused.clearFocus()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        /** Clave con la que el detalle del álbum recibe la canción agregada (un Bundle). */
        const val RESULT_ADDED_TRACK = "added_track"
        const val RESULT_TRACK_ID = "trackId"
        const val RESULT_TRACK_NAME = "trackName"

        @get:StringRes
        private val TrackFormError.messageRes: Int
            get() = when (this) {
                TrackFormError.REQUIRED -> R.string.add_track_error_required
                TrackFormError.INVALID_DURATION -> R.string.add_track_error_duration
            }
    }
}
