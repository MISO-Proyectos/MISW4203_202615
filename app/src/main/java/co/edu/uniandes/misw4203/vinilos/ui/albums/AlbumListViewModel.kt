package co.edu.uniandes.misw4203.vinilos.ui.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
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
 * ViewModel del catálogo de álbumes (HU01). Pide los álbumes al repositorio y
 * expone el estado de la pantalla. Como sobrevive a las rotaciones, los álbumes
 * no se vuelven a pedir al girar el teléfono.
 */
class AlbumListViewModel(
    private val albumRepository: AlbumRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AlbumListUiState>(AlbumListUiState.Loading)
    val uiState: StateFlow<AlbumListUiState> = _uiState.asStateFlow()

    init {
        loadAlbums()
    }

    /** Pide los álbumes. Se llama al abrir la pantalla y desde el botón "Reintentar". */
    fun loadAlbums() {
        _uiState.value = AlbumListUiState.Loading
        viewModelScope.launch {
            _uiState.value = albumRepository.getAlbums().fold(
                onSuccess = { albums ->
                    if (albums.isEmpty()) AlbumListUiState.Empty
                    else AlbumListUiState.Success(albums)
                },
                onFailure = { error ->
                    AlbumListUiState.Error(error.message.orEmpty())
                }
            )
        }
    }

    companion object {
        /** Crea el ViewModel con el repositorio de AppContainer. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as VinilosApplication
                AlbumListViewModel(app.appContainer.albumRepository)
            }
        }
    }
}
