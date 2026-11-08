package co.edu.uniandes.misw4203.vinilos.ui.artists.detail

import co.edu.uniandes.misw4203.vinilos.data.model.ArtistDetail

/** Estado de la pantalla de detalle de un artista (HU04). */
sealed interface ArtistDetailUiState {
    data object Loading : ArtistDetailUiState
    data class Success(val artistDetail: ArtistDetail) : ArtistDetailUiState
    data class Error(val message: String) : ArtistDetailUiState
}
