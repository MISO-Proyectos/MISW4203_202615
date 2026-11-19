package co.edu.uniandes.misw4203.vinilos.ui.albums.comments

import co.edu.uniandes.misw4203.vinilos.data.model.Album
import co.edu.uniandes.misw4203.vinilos.data.model.Collector

/**
 * Estado de la pantalla "Comentar álbum" (HU09). El texto del comentario lo guarda su
 * campo (y el ViewModel en SavedStateHandle); aquí van el álbum de la tarjeta, la lista
 * de coleccionistas, lo elegido y los errores.
 */
data class AddCommentUiState(
    /** Álbum de la tarjeta superior; null mientras carga o si no se pudo cargar. */
    val album: Album? = null,
    val collectors: List<Collector> = emptyList(),
    val isLoadingCollectors: Boolean = true,
    /** true si no cargó la lista de coleccionistas: el campo lo dice y deja reintentar. */
    val collectorsLoadFailed: Boolean = false,
    val selectedCollectorId: Int? = null,
    /** Calificación elegida (1 a 5); 0 mientras no se elige ninguna. */
    val rating: Int = 0,
    /** Errores por campo; solo se llenan después de tocar "Publicar comentario". */
    val errors: Map<CommentFormField, CommentFormError> = emptyMap(),
    /** true mientras se envía al backend: el formulario queda bloqueado. */
    val isSaving: Boolean = false
) {
    /** Muestra el aviso "Completa los campos obligatorios para guardar." */
    val showErrorBanner: Boolean get() = errors.isNotEmpty()
}

/** Avisos de una sola vez que el Fragment atiende y luego olvida. */
sealed interface AddCommentEvent {

    /** El backend guardó el comentario; el Fragment vuelve al detalle del álbum. */
    data object Published : AddCommentEvent

    /** No se pudo publicar; [message] se muestra al usuario. */
    data class Failed(val message: String) : AddCommentEvent
}
