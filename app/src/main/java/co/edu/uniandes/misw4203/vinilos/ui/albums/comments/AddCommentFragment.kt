package co.edu.uniandes.misw4203.vinilos.ui.albums.comments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
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
import co.edu.uniandes.misw4203.vinilos.databinding.FragmentAddCommentBinding
import co.edu.uniandes.misw4203.vinilos.ui.common.CollectorFieldBinder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Pantalla "Comentar álbum" (HU09). Le pasa al ViewModel lo que elige o escribe el usuario,
 * dibuja la tarjeta del álbum y los errores y, cuando el comentario se publica, vuelve al
 * detalle dejando el resultado en [RESULT_ADDED_COMMENT] para que el detalle lo recargue.
 */
class AddCommentFragment : Fragment() {

    private var _binding: FragmentAddCommentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddCommentViewModel by viewModels { AddCommentViewModel.Factory }

    private lateinit var collectorField: CollectorFieldBinder

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddCommentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        collectorField = CollectorFieldBinder(
            layout = binding.collectorField.collectorLayout,
            input = binding.collectorField.collectorInput,
            onSelected = viewModel::onCollectorSelected,
            onRetry = viewModel::retryCollectors
        )

        // La calificación la pinta render() desde el ViewModel; si la vista la restaurara
        // sola, el listener de abajo no se enteraría del valor restaurado
        binding.ratingBar.isSaveEnabled = false
        binding.ratingBar.setOnRatingBarChangeListener { _, rating, fromUser ->
            if (fromUser) viewModel.onRatingChanged(rating.toInt())
        }
        binding.descriptionInput.doAfterTextChanged {
            viewModel.onDescriptionChanged(it?.toString().orEmpty())
        }
        binding.saveButton.setOnClickListener { save() }
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

    private fun render(state: AddCommentUiState) {
        val bannerWasVisible = binding.errorBanner.isVisible
        binding.errorBanner.isVisible = state.showErrorBanner
        // El aviso está arriba: si recién aparece, se sube para que el usuario lo vea
        if (state.showErrorBanner && !bannerWasVisible) binding.addCommentScroll.smoothScrollTo(0, 0)

        binding.albumSummary.isVisible = state.album != null
        state.album?.let(::showAlbum)

        collectorField.render(
            collectors = state.collectors,
            selectedId = state.selectedCollectorId,
            loadFailed = state.collectorsLoadFailed,
            error = state.errors[CommentFormField.COLLECTOR]?.let { getString(R.string.collector_field_error_required) }
        )

        showRating(state.rating)
        binding.ratingError.isVisible = CommentFormField.RATING in state.errors

        val descriptionError = when (state.errors[CommentFormField.DESCRIPTION]) {
            CommentFormError.REQUIRED -> getString(R.string.add_comment_error_description_required)
            CommentFormError.TOO_LONG -> getString(R.string.add_comment_error_description_too_long)
            null -> null
        }
        if (binding.descriptionLayout.error?.toString() != descriptionError) {
            binding.descriptionLayout.error = descriptionError
        }

        // Los campos siguen activos mientras guarda (TalkBack anunciaría cada uno como
        // "inhabilitado" y taparía el aviso de éxito); solo se bloquean los botones.
        val enabled = !state.isSaving
        binding.saveButton.isEnabled = enabled
        binding.cancelButton.isEnabled = enabled
        binding.savingIndicator.isVisible = state.isSaving
    }

    /** Pinta las estrellas elegidas y deja que TalkBack lea la calificación ("4 de 5 estrellas"). */
    private fun showRating(rating: Int) {
        if (binding.ratingBar.rating.toInt() != rating) binding.ratingBar.rating = rating.toFloat()
        binding.ratingBar.contentDescription = if (rating == 0) {
            getString(R.string.add_comment_rating_none)
        } else {
            getString(R.string.add_comment_rating_value, rating)
        }
    }

    private fun showAlbum(album: Album) {
        binding.albumName.text = album.name
        val tracks = resources.getQuantityString(
            R.plurals.add_comment_album_tracks, album.tracks.size, album.tracks.size
        )
        val performers = album.performerNames.ifBlank { getString(R.string.album_unknown_performer) }
        binding.albumInfo.text = getString(R.string.add_comment_album_info, performers, tracks)
        Glide.with(binding.albumCover)
            .load(album.cover.ifBlank { null })
            .placeholder(R.drawable.ic_album)
            .error(R.drawable.ic_album)
            .override(resources.getDimensionPixelSize(R.dimen.add_track_cover_size))
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .into(binding.albumCover)
    }

    private fun handleEvent(event: AddCommentEvent) {
        when (event) {
            AddCommentEvent.Published -> {
                val navController = findNavController()
                navController.previousBackStackEntry?.savedStateHandle?.set(RESULT_ADDED_COMMENT, true)
                navController.popBackStack()
            }
            is AddCommentEvent.Failed ->
                Snackbar.make(
                    binding.root,
                    event.message.ifBlank { getString(R.string.add_comment_error_default) },
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
        /** Clave con la que el detalle del álbum sabe que se publicó un comentario (un Boolean). */
        const val RESULT_ADDED_COMMENT = "added_comment"
    }
}
