package co.edu.uniandes.misw4203.vinilos.ui.albums.detail

import co.edu.uniandes.misw4203.vinilos.data.model.Album

/** Estados posibles de la pantalla de detalle de un álbum (HU02). */
sealed interface AlbumDetailUiState {

    /** Se está pidiendo el álbum al backend. */
    data object Loading : AlbumDetailUiState

    /** El álbum llegó completo. */
    data class Success(val album: Album) : AlbumDetailUiState

    /** No se pudo cargar el álbum; [message] se muestra al usuario. */
    data class Error(val message: String) : AlbumDetailUiState
}
