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
 * ViewModel del detalle de un álbum (HU02 / HU09).
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

    /** Canción recién agregada (HU08) que se resalta en la lista. */
    private var highlightedTrackId: Int? = null

    /** Id del álbum, para abrir el formulario "Agregar track". */
    val currentAlbumId: Int get() = albumId

    /** Pide el álbum. Se llama al abrir la pantalla y desde el botón "Reintentar". */
    fun loadAlbum() {
        _uiState.value = AlbumDetailUiState.Loading
        viewModelScope.launch {
            _uiState.value = albumRepository.getAlbum(albumId).fold(
                onSuccess = { AlbumDetailUiState.Success(it, highlightedTrackId) },
                onFailure = { AlbumDetailUiState.Error(it.message.orEmpty()) }
            )
        }
    }

    /**
     * HU08: se agregó la canción [trackId]. Se vuelve a pedir el álbum sin pasar por
     * Loading, para que la pantalla no parpadee ni pierda el scroll, y se resalta la
     * canción nueva. Si la recarga falla se deja el álbum que ya se veía.
     */
    fun onTrackAdded(trackId: Int) {
        highlightedTrackId = trackId
        viewModelScope.launch {
            albumRepository.getAlbum(albumId).onSuccess {
                _uiState.value = AlbumDetailUiState.Success(it, trackId)
            }
        }
    }

    /** HU09: agrega un comentario al álbum y recarga sus detalles si fue exitoso. */
    fun addComment(
        collectorId: Int = DEFAULT_COLLECTOR_ID,
        rating: Int,
        description: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            val result = albumRepository.addComment(albumId, collectorId, rating, description)
            if (result.isSuccess) {
                loadAlbum()
                onResult(Result.success(Unit))
            } else {
                onResult(Result.failure(result.exceptionOrNull() ?: Exception()))
            }
        }
    }

    companion object {
        /** Nombre del argumento en nav_graph.xml. */
        const val ARG_ALBUM_ID = "albumId"

        /** ID de coleccionista por defecto (Manolo Bellon). */
        const val DEFAULT_COLLECTOR_ID = 100

        /** Crea el ViewModel con los argumentos de navegación y el repositorio de AppContainer. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as VinilosApplication
                AlbumDetailViewModel(createSavedStateHandle(), app.appContainer.albumRepository)
            }
        }
    }
}
