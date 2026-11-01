package co.edu.uniandes.misw4203.vinilos.ui.albums

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import co.edu.uniandes.misw4203.vinilos.R
import co.edu.uniandes.misw4203.vinilos.databinding.FragmentAlbumListBinding
import co.edu.uniandes.misw4203.vinilos.ui.role.SessionViewModel
import co.edu.uniandes.misw4203.vinilos.ui.role.UserRole
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Pantalla del catálogo de álbumes (HU01). Solo dibuja el estado que entrega
 * AlbumListViewModel y le avisa cuando el usuario toca "Reintentar".
 * El botón "Agregar álbum" depende del rol guardado en SessionViewModel.
 */
class AlbumListFragment : Fragment() {

    private var _binding: FragmentAlbumListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AlbumListViewModel by viewModels { AlbumListViewModel.Factory }

    /** Rol elegido en la primera pantalla; es el mismo ViewModel que usa MainActivity. */
    private val session: SessionViewModel by activityViewModels()

    private val countAdapter = AlbumCountAdapter()
    private val albumAdapter = AlbumAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlbumListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.albumList.adapter = ConcatAdapter(countAdapter, albumAdapter)
        binding.retryButton.setOnClickListener { viewModel.loadAlbums() }
        binding.addAlbumButton.setOnClickListener {
            // TODO: navegar al formulario de creación de álbum cuando exista su HU
            Snackbar.make(binding.root, R.string.add_album_coming_soon, Snackbar.LENGTH_SHORT)
                .setAnchorView(binding.addAlbumButton)
                .show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { session.role.collect(::renderRole) }
            }
        }
    }

    private fun renderRole(role: UserRole?) {
        val canAdd = role?.canAddAlbums == true
        binding.addAlbumButton.isVisible = canAdd
        // Espacio extra al final de la lista para que el botón no tape el último álbum
        val extraBottom = if (canAdd) resources.getDimensionPixelSize(R.dimen.fab_list_clearance) else 0
        binding.albumList.updatePadding(
            bottom = resources.getDimensionPixelSize(R.dimen.list_bottom_padding) + extraBottom
        )
    }

    private fun render(state: AlbumListUiState) {
        binding.loadingIndicator.isVisible = state is AlbumListUiState.Loading
        binding.albumList.isVisible = state is AlbumListUiState.Success
        binding.messageContainer.isVisible =
            state is AlbumListUiState.Empty || state is AlbumListUiState.Error
        binding.retryButton.isVisible = state is AlbumListUiState.Error

        when (state) {
            is AlbumListUiState.Success -> {
                countAdapter.count = state.albums.size
                albumAdapter.submitList(state.albums)
            }
            AlbumListUiState.Empty ->
                binding.messageText.setText(R.string.albums_empty)
            is AlbumListUiState.Error ->
                binding.messageText.text =
                    state.message.ifBlank { getString(R.string.albums_error_default) }
            AlbumListUiState.Loading -> Unit
        }
    }

    override fun onDestroyView() {
        binding.albumList.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
