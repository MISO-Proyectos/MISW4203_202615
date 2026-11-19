package co.edu.uniandes.misw4203.vinilos.ui.albums.collection

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
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
import co.edu.uniandes.misw4203.vinilos.data.model.CollectorAlbumStatus
import co.edu.uniandes.misw4203.vinilos.databinding.FragmentAddToCollectionBinding
import co.edu.uniandes.misw4203.vinilos.ui.common.CollectorFieldBinder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Pantalla "Agregar a colección" (HU11). Le pasa al ViewModel lo que elige o escribe el
 * usuario, dibuja la tarjeta del álbum y los errores y, cuando el álbum se guarda, vuelve al
 * detalle dejando el resultado en [RESULT_ADDED_TO_COLLECTION] para que el detalle lo muestre.
 */
class AddToCollectionFragment : Fragment() {

    private var _binding: FragmentAddToCollectionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddToCollectionViewModel by viewModels { AddToCollectionViewModel.Factory }

    private lateinit var collectorField: CollectorFieldBinder

    /** true mientras render() pinta el estado elegido: evita que el listener del grupo responda. */
    private var renderingStatus = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddToCollectionBinding.inflate(inflater, container, false)
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
        setUpStatusGroup()

        binding.priceInput.doAfterTextChanged { viewModel.onPriceChanged(it?.toString().orEmpty()) }
        binding.priceInput.setOnEditorActionListener { _, _, _ ->
            save()
            true
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

    /**
     * Activo o Inactivo, de selección única. El estado elegido lo pinta render() desde el
     * ViewModel; por eso el grupo y sus botones no restauran su selección solos (si lo
     * hicieran, el listener se dispararía dos veces al girar la pantalla).
     */
    private fun setUpStatusGroup() {
        binding.statusGroup.isSaveEnabled = false
        binding.statusActiveButton.isSaveEnabled = false
        binding.statusInactiveButton.isSaveEnabled = false
        binding.statusGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (renderingStatus || !isChecked) return@addOnButtonCheckedListener
            viewModel.onStatusSelected(
                if (checkedId == R.id.statusActiveButton) CollectorAlbumStatus.ACTIVE
                else CollectorAlbumStatus.INACTIVE
            )
        }
    }

    private fun save() {
        hideKeyboard()
        viewModel.save()
    }

    private fun render(state: AddToCollectionUiState) {
        val bannerWasVisible = binding.errorBanner.isVisible
        binding.errorBanner.isVisible = state.showErrorBanner
        // El aviso está arriba: si recién aparece, se sube para que el usuario lo vea
        if (state.showErrorBanner && !bannerWasVisible) binding.addToCollectionScroll.smoothScrollTo(0, 0)

        binding.albumSummary.isVisible = state.album != null
        state.album?.let(::showAlbum)

        val collectorError = state.duplicateCollectorName
            ?.let { getString(R.string.add_to_collection_error_duplicate, it) }
            ?: state.errors[CollectionFormField.COLLECTOR]
                ?.let { getString(R.string.collector_field_error_required) }
        collectorField.render(
            collectors = state.collectors,
            selectedId = state.selectedCollectorId,
            loadFailed = state.collectorsLoadFailed,
            error = collectorError
        )

        val priceError = state.errors[CollectionFormField.PRICE]
            ?.let { getString(R.string.add_to_collection_error_price) }
        if (binding.priceLayout.error?.toString() != priceError) binding.priceLayout.error = priceError

        showStatus(state.status)
        binding.statusError.isVisible = CollectionFormField.STATUS in state.errors

        val enabled = !state.isSaving
        binding.collectorField.collectorLayout.isEnabled = enabled
        binding.priceLayout.isEnabled = enabled
        binding.statusGroup.isEnabled = enabled
        binding.statusActiveButton.isEnabled = enabled
        binding.statusInactiveButton.isEnabled = enabled
        binding.saveButton.isEnabled = enabled
        binding.cancelButton.isEnabled = enabled
        binding.savingIndicator.isVisible = state.isSaving
    }

    private fun showStatus(status: CollectorAlbumStatus?) {
        val buttonId = when (status) {
            CollectorAlbumStatus.ACTIVE -> R.id.statusActiveButton
            CollectorAlbumStatus.INACTIVE -> R.id.statusInactiveButton
            null -> View.NO_ID
        }
        if (binding.statusGroup.checkedButtonId == buttonId) return
        renderingStatus = true
        if (buttonId == View.NO_ID) binding.statusGroup.clearChecked() else binding.statusGroup.check(buttonId)
        renderingStatus = false
    }

    private fun showAlbum(album: Album) {
        binding.albumName.text = album.name
        val tracks = resources.getQuantityString(
            R.plurals.add_to_collection_album_tracks, album.tracks.size, album.tracks.size
        )
        val performers = album.performerNames.ifBlank { getString(R.string.album_unknown_performer) }
        binding.albumInfo.text = getString(R.string.add_to_collection_album_info, performers, tracks)
        Glide.with(binding.albumCover)
            .load(album.cover.ifBlank { null })
            .placeholder(R.drawable.ic_album)
            .error(R.drawable.ic_album)
            .override(resources.getDimensionPixelSize(R.dimen.add_track_cover_size))
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .into(binding.albumCover)
    }

    private fun handleEvent(event: AddToCollectionEvent) {
        when (event) {
            is AddToCollectionEvent.Added -> {
                val navController = findNavController()
                navController.previousBackStackEntry?.savedStateHandle?.set(
                    RESULT_ADDED_TO_COLLECTION,
                    bundleOf(
                        RESULT_ALBUM_NAME to event.albumName.ifBlank {
                            getString(R.string.add_to_collection_album_fallback)
                        },
                        RESULT_COLLECTOR_NAME to event.collectorName
                    )
                )
                navController.popBackStack()
            }
            is AddToCollectionEvent.Failed ->
                Snackbar.make(
                    binding.root,
                    event.message.ifBlank { getString(R.string.add_to_collection_error_default) },
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
        /** Clave con la que el detalle del álbum recibe el resultado (un Bundle). */
        const val RESULT_ADDED_TO_COLLECTION = "added_to_collection"
        const val RESULT_ALBUM_NAME = "albumName"
        const val RESULT_COLLECTOR_NAME = "collectorName"
    }
}
