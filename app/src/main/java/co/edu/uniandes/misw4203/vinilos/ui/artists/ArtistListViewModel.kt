package co.edu.uniandes.misw4203.vinilos.ui.artists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import co.edu.uniandes.misw4203.vinilos.VinilosApplication
import co.edu.uniandes.misw4203.vinilos.data.model.Artist
import co.edu.uniandes.misw4203.vinilos.data.repository.ArtistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel del listado de artistas (HU03). Pide la lista al repositorio y
 * expone el estado de la pantalla. Como sobrevive a las rotaciones, los artistas
 * no se vuelven a pedir al girar el teléfono.
 */
class ArtistListViewModel(
    private val artistRepository: ArtistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ArtistListUiState>(ArtistListUiState.Loading)
    val uiState: StateFlow<ArtistListUiState> = _uiState.asStateFlow()

    init {
        loadArtists()
    }

    /** Pide los artistas. Se llama al abrir la pantalla y desde el botón "Reintentar". */
    fun loadArtists() {
        _uiState.value = ArtistListUiState.Loading
        viewModelScope.launch {
            _uiState.value = artistRepository.getArtists().fold(
                onSuccess = ::listState,
                onFailure = { error ->
                    ArtistListUiState.Error(error.message.orEmpty())
                }
            )
        }
    }

    /**
     * Recarga al deslizar hacia abajo. Si ya hay una lista, se queda en pantalla mientras
     * llegan los datos nuevos y, si la recarga falla, se conserva con un aviso del error.
     * Desde Vacío o Error se comporta como [loadArtists].
     */
    fun refresh() {
        val current = _uiState.value
        if (current !is ArtistListUiState.Success) {
            loadArtists()
            return
        }
        if (current.isRefreshing) return

        _uiState.value = current.copy(isRefreshing = true, refreshError = null)
        viewModelScope.launch {
            _uiState.value = artistRepository.getArtists().fold(
                onSuccess = ::listState,
                onFailure = { error ->
                    current.copy(isRefreshing = false, refreshError = error.message.orEmpty())
                }
            )
        }
    }

    /** La pantalla ya mostró el aviso del error de recarga; no se vuelve a mostrar. */
    fun refreshErrorShown() {
        val current = _uiState.value
        if (current is ArtistListUiState.Success && current.refreshError != null) {
            _uiState.value = current.copy(refreshError = null)
        }
    }

    private fun listState(artists: List<Artist>): ArtistListUiState =
        if (artists.isEmpty()) ArtistListUiState.Empty
        else ArtistListUiState.Success(artists)

    companion object {
        /** Crea el ViewModel con el repositorio de AppContainer. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as VinilosApplication
                ArtistListViewModel(app.appContainer.artistRepository)
            }
        }
    }
}
