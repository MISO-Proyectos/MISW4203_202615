package co.edu.uniandes.misw4203.vinilos.ui.artists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import co.edu.uniandes.misw4203.vinilos.R
import co.edu.uniandes.misw4203.vinilos.data.model.Artist
import co.edu.uniandes.misw4203.vinilos.databinding.FragmentArtistListBinding
import co.edu.uniandes.misw4203.vinilos.ui.artists.detail.ArtistDetailViewModel
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Pantalla del listado de artistas (HU03). Solo dibuja el estado que entrega
 * ArtistListViewModel y le avisa cuando el usuario toca "Reintentar" o desliza hacia abajo para recargar.
 * Es igual para visitantes y coleccionistas: no depende del rol.
 */
class ArtistListFragment : Fragment() {

    private var _binding: FragmentArtistListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ArtistListViewModel by viewModels { ArtistListViewModel.Factory }

    private val countAdapter = ArtistCountAdapter()
    private val artistAdapter = ArtistAdapter(::openArtist)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArtistListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.artistList.adapter = ConcatAdapter(countAdapter, artistAdapter)
        binding.retryButton.setOnClickListener { viewModel.loadArtists(forceRefresh = true) }
        setUpSwipeRefresh()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun setUpSwipeRefresh() {
        val swipe = binding.swipeRefresh
        swipe.setOnRefreshListener { viewModel.refresh() }
        swipe.setColorSchemeColors(
            MaterialColors.getColor(swipe, androidx.appcompat.R.attr.colorPrimary)
        )
        swipe.setProgressBackgroundColorSchemeColor(
            MaterialColors.getColor(swipe, com.google.android.material.R.attr.colorSurfaceContainerHigh)
        )
        // El hijo directo es un FrameLayout que no se desplaza: hay que preguntarle a la lista
        // si puede subir, para que deslizar hacia abajo a mitad de la lista la desplace y no recargue.
        swipe.setOnChildScrollUpCallback { _, _ ->
            binding.artistList.isVisible && binding.artistList.canScrollVertically(-1)
        }
    }

    /** Navega al detalle del artista (HU04). */
    private fun openArtist(artist: Artist) {
        findNavController().navigate(
            R.id.action_artistList_to_artistDetail,
            bundleOf(
                ArtistDetailViewModel.ARG_ARTIST_ID to artist.id,
                ArtistDetailViewModel.ARG_ARTIST_TYPE to artist.type.name
            )
        )
    }

    private fun render(state: ArtistListUiState) {
        binding.loadingIndicator.isVisible = state is ArtistListUiState.Loading
        binding.artistList.isVisible = state is ArtistListUiState.Success
        binding.messageContainer.isVisible =
            state is ArtistListUiState.Empty || state is ArtistListUiState.Error
        binding.retryButton.isVisible = state is ArtistListUiState.Error
        // Mientras se carga a pantalla completa no tiene sentido deslizar para recargar
        binding.swipeRefresh.isEnabled = state !is ArtistListUiState.Loading
        binding.swipeRefresh.isRefreshing =
            state is ArtistListUiState.Success && state.isRefreshing

        when (state) {
            is ArtistListUiState.Success -> {
                countAdapter.count = state.artists.size
                artistAdapter.submitList(state.artists)
                state.refreshError?.let(::showRefreshError)
            }
            ArtistListUiState.Empty -> {
                binding.messageIcon.setImageResource(R.drawable.ic_inbox)
                binding.messageTitle.setText(R.string.artists_empty_title)
                binding.messageText.setText(R.string.artists_empty_subtitle)
            }
            is ArtistListUiState.Error -> {
                binding.messageIcon.setImageResource(R.drawable.ic_cloud_off)
                binding.messageTitle.setText(R.string.artists_error_title)
                binding.messageText.text =
                    state.message.ifBlank { getString(R.string.artists_error_default) }
            }
            ArtistListUiState.Loading -> Unit
        }
    }

    private fun showRefreshError(message: String) {
        Snackbar.make(
            binding.root,
            message.ifBlank { getString(R.string.artists_error_default) },
            Snackbar.LENGTH_LONG
        ).show()
        viewModel.refreshErrorShown()
    }

    override fun onDestroyView() {
        binding.artistList.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
