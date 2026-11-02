package co.edu.uniandes.misw4203.vinilos.ui.albums.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import co.edu.uniandes.misw4203.vinilos.VinilosApplication
import co.edu.uniandes.misw4203.vinilos.data.repository.AlbumRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel del detalle de un álbum (HU02). El id del álbum llega en los argumentos
 * de navegación, que Android copia en [SavedStateHandle]; así el ViewModel no depende
 * del Fragment y sigue funcionando si Android cierra y restaura el proceso.
 */
class AlbumDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val albumRepository: AlbumRepository
) : ViewModel() {

    private val albumId: Int = requireNotNull(savedStateHandle.get<Int>(ARG_ALBUM_ID)) {
        "AlbumDetailViewModel necesita el argumento '$ARG_ALBUM_ID'"
    }

    private val _uiState = MutableStateFlow<AlbumDetailUiState>(AlbumDetailUiState.Loading)
    val uiState: StateFlow<AlbumDetailUiState> = _uiState.asStateFlow()

    init {
        loadAlbum()
    }

    /** Pide el álbum. Se llama al abrir la pantalla y desde el botón "Reintentar". */
    fun loadAlbum() {
        _uiState.value = AlbumDetailUiState.Loading
        viewModelScope.launch {
            _uiState.value = albumRepository.getAlbum(albumId).fold(
                onSuccess = { AlbumDetailUiState.Success(it) },
                onFailure = { AlbumDetailUiState.Error(it.message.orEmpty()) }
            )
        }
    }

    companion object {
        /** Nombre del argumento en nav_graph.xml. */
        const val ARG_ALBUM_ID = "albumId"

        /** Crea el ViewModel con los argumentos de navegación y el repositorio de AppContainer. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as VinilosApplication
                AlbumDetailViewModel(createSavedStateHandle(), app.appContainer.albumRepository)
            }
        }
    }
}
