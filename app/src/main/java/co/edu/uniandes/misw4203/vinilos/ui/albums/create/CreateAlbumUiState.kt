package co.edu.uniandes.misw4203.vinilos.ui.albums.create

import co.edu.uniandes.misw4203.vinilos.data.model.AlbumGenre
import co.edu.uniandes.misw4203.vinilos.data.model.RecordLabel
import co.edu.uniandes.misw4203.vinilos.data.model.ReleaseDate

/**
 * Estado de la pantalla "Agregar álbum" (HU07). Los textos (nombre, URL, descripción)
 * los guarda cada campo; aquí solo van los valores elegidos en selectores y los errores.
 */
data class CreateAlbumUiState(
    val releaseDate: ReleaseDate? = null,
    val genre: AlbumGenre? = null,
    val recordLabel: RecordLabel? = null,
    /** Errores por campo; solo se llenan después de tocar "Guardar álbum". */
    val errors: Map<AlbumFormField, AlbumFormError> = emptyMap(),
    /** true mientras se envía el álbum al backend: el formulario queda bloqueado. */
    val isSaving: Boolean = false
)

/** Avisos de una sola vez que el Fragment muestra o atiende y luego olvida. */
sealed interface CreateAlbumEvent {

    /** El backend guardó el álbum; el Fragment vuelve al catálogo. */
    data class Created(val albumName: String) : CreateAlbumEvent

    /** No se pudo guardar; [message] se muestra al usuario. */
    data class Failed(val message: String) : CreateAlbumEvent
}
