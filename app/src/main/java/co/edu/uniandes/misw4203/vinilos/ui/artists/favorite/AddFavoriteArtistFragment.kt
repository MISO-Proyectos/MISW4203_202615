package co.edu.uniandes.misw4203.vinilos.ui.artists.favorite

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import co.edu.uniandes.misw4203.vinilos.R
import co.edu.uniandes.misw4203.vinilos.databinding.FragmentAddFavoriteArtistBinding
import co.edu.uniandes.misw4203.vinilos.ui.artists.labelRes
import co.edu.uniandes.misw4203.vinilos.ui.common.CollectorFieldBinder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Pantalla "Agregar a favoritos" (HU10). Le pasa al ViewModel el coleccionista que elige el
 * usuario, dibuja la tarjeta del artista y los errores y, cuando el favorito se guarda, vuelve
 * al detalle del artista dejando el resultado en [RESULT_ADDED_FAVORITE] para que el detalle
 * lo muestre.
 */
class AddFavoriteArtistFragment : Fragment() {

    private var _binding: FragmentAddFavoriteArtistBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddFavoriteArtistViewModel by viewModels { AddFavoriteArtistViewModel.Factory }

    private lateinit var collectorField: CollectorFieldBinder

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddFavoriteArtistBinding.inflate(inflater, container, false)
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
        // Texto de ayuda propio de este formulario; el campo común trae uno genérico
        binding.collectorField.collectorLayout.helperText = getString(R.string.add_favorite_collector_helper)

        binding.saveButton.setOnClickListener {
            hideKeyboard()
            viewModel.save()
        }
        binding.cancelButton.setOnClickListener { findNavController().navigateUp() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.events.collect(::handleEvent) }
            }
        }
    }

    private fun render(state: AddFavoriteArtistUiState) {
        val bannerWasVisible = binding.errorBanner.isVisible
        binding.errorBanner.isVisible = state.showErrorBanner
        // El aviso está arriba: si recién aparece, se sube para que el usuario lo vea
        if (state.showErrorBanner && !bannerWasVisible) binding.addFavoriteScroll.smoothScrollTo(0, 0)

        binding.artistName.text = state.artistName
        binding.artistInfo.text = getString(
            R.string.add_favorite_artist_info,
            getString(state.artistType.labelRes),
            resources.getQuantityString(R.plurals.artist_albums_count, state.albumCount, state.albumCount)
        )

        val collectorError = state.duplicateCollectorName
            ?.let { getString(R.string.add_favorite_error_duplicate, it) }
            ?: state.errors[FavoriteFormField.COLLECTOR]
                ?.let { getString(R.string.collector_field_error_required) }
        collectorField.render(
            collectors = state.collectors,
            selectedId = state.selectedCollectorId,
            loadFailed = state.collectorsLoadFailed,
            error = collectorError
        )

        val enabled = !state.isSaving
        binding.collectorField.collectorLayout.isEnabled = enabled
        binding.saveButton.isEnabled = enabled
        binding.cancelButton.isEnabled = enabled
        binding.savingIndicator.isVisible = state.isSaving
    }

    private fun handleEvent(event: AddFavoriteArtistEvent) {
        when (event) {
            is AddFavoriteArtistEvent.Added -> {
                val navController = findNavController()
                navController.previousBackStackEntry?.savedStateHandle?.set(
                    RESULT_ADDED_FAVORITE,
                    bundleOf(
                        RESULT_ARTIST_NAME to event.artistName,
                        RESULT_COLLECTOR_NAME to event.collectorName
                    )
                )
                navController.popBackStack()
            }
            is AddFavoriteArtistEvent.Failed ->
                Snackbar.make(
                    binding.root,
                    event.message.ifBlank { getString(R.string.add_favorite_error_default) },
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
        /** Clave con la que el detalle del artista recibe el resultado (un Bundle). */
        const val RESULT_ADDED_FAVORITE = "added_favorite"
        const val RESULT_ARTIST_NAME = "artistName"
        const val RESULT_COLLECTOR_NAME = "collectorName"
    }
}
