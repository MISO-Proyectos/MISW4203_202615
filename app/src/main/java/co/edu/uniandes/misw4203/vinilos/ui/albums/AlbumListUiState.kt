package co.edu.uniandes.misw4203.vinilos.ui.albums

import co.edu.uniandes.misw4203.vinilos.data.model.Album

/** Estados posibles de la pantalla del catálogo de álbumes (HU01). */
sealed interface AlbumListUiState {

    /** Se están pidiendo los álbumes al backend. */
    data object Loading : AlbumListUiState

    /** El backend respondió con al menos un álbum. */
    data class Success(val albums: List<Album>) : AlbumListUiState

    /** El backend respondió bien, pero el catálogo no tiene álbumes. */
    data object Empty : AlbumListUiState

    /** No se pudieron cargar los álbumes; [message] se muestra al usuario. */
    data class Error(val message: String) : AlbumListUiState
}
