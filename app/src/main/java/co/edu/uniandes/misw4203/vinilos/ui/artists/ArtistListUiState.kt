package co.edu.uniandes.misw4203.vinilos.ui.artists

import co.edu.uniandes.misw4203.vinilos.data.model.Artist

/** Estados posibles de la pantalla del listado de artistas (HU03). */
sealed interface ArtistListUiState {

    /** Se están pidiendo los artistas al backend. */
    data object Loading : ArtistListUiState

    /**
     * El backend respondió con al menos un artista.
     * [isRefreshing] es true mientras se recarga al deslizar hacia abajo (la lista sigue visible).
     * [refreshError] trae el mensaje si esa recarga falló; la lista anterior se conserva.
     */
    data class Success(
        val artists: List<Artist>,
        val isRefreshing: Boolean = false,
        val refreshError: String? = null
    ) : ArtistListUiState

    /** El backend respondió bien, pero no hay artistas registrados. */
    data object Empty : ArtistListUiState

    /** No se pudieron cargar los artistas; [message] se muestra al usuario. */
    data class Error(val message: String) : ArtistListUiState
}
