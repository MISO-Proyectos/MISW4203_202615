package co.edu.uniandes.misw4203.vinilos.ui.artists.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import co.edu.uniandes.misw4203.vinilos.VinilosApplication
import co.edu.uniandes.misw4203.vinilos.data.model.ArtistType
import co.edu.uniandes.misw4203.vinilos.data.repository.ArtistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel del detalle de un artista (HU04).
 */
class ArtistDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val artistRepository: ArtistRepository
) : ViewModel() {

    private val artistId: Int = requireNotNull(savedStateHandle.get<Int>(ARG_ARTIST_ID)) {
        "ArtistDetailViewModel necesita '$ARG_ARTIST_ID'"
    }

    private val artistTypeString: String = requireNotNull(savedStateHandle.get<String>(ARG_ARTIST_TYPE)) {
        "ArtistDetailViewModel necesita '$ARG_ARTIST_TYPE'"
    }

    private val artistType: ArtistType = try {
        ArtistType.valueOf(artistTypeString)
    } catch (e: Exception) {
        ArtistType.MUSICIAN
    }

    private val _uiState = MutableStateFlow<ArtistDetailUiState>(ArtistDetailUiState.Loading)
    val uiState: StateFlow<ArtistDetailUiState> = _uiState.asStateFlow()

    init {
        loadArtistDetail()
    }

    fun loadArtistDetail() {
        _uiState.value = ArtistDetailUiState.Loading
        viewModelScope.launch {
            _uiState.value = artistRepository.getArtistDetail(artistId, artistType).fold(
                onSuccess = { ArtistDetailUiState.Success(it) },
                onFailure = { ArtistDetailUiState.Error(it.message.orEmpty()) }
            )
        }
    }

    companion object {
        const val ARG_ARTIST_ID = "artistId"
        const val ARG_ARTIST_TYPE = "artistType"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as VinilosApplication
                ArtistDetailViewModel(createSavedStateHandle(), app.appContainer.artistRepository)
            }
        }
    }
}
