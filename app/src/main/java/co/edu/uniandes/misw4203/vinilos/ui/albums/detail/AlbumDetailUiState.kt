package co.edu.uniandes.misw4203.vinilos.ui.albums.detail

import co.edu.uniandes.misw4203.vinilos.data.model.Album

/** Estados posibles de la pantalla de detalle de un álbum (HU02). */
sealed interface AlbumDetailUiState {

    /** Se está pidiendo el álbum al backend. */
    data object Loading : AlbumDetailUiState

    /**
     * El álbum llegó completo. [highlightedTrackId] es la canción recién agregada (HU08),
     * que se resalta en la lista; null si no hay ninguna.
     */
    data class Success(val album: Album, val highlightedTrackId: Int? = null) : AlbumDetailUiState

    /** No se pudo cargar el álbum; [message] se muestra al usuario. */
    data class Error(val message: String) : AlbumDetailUiState
}
