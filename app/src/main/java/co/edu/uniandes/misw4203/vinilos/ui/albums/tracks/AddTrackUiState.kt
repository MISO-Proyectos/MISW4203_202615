package co.edu.uniandes.misw4203.vinilos.ui.albums.tracks

import co.edu.uniandes.misw4203.vinilos.data.model.Album

/**
 * Estado de la pantalla "Agregar track" (HU08). Los textos (nombre y duración) los
 * guarda cada campo; aquí van el álbum de la tarjeta superior y los errores.
 */
data class AddTrackUiState(
    /** Álbum al que se agrega la canción; null mientras carga o si no se pudo cargar. */
    val album: Album? = null,
    /** Errores por campo; solo se llenan después de tocar "Guardar track". */
    val errors: Map<TrackFormField, TrackFormError> = emptyMap(),
    /** true mientras se envía la canción al backend: el formulario queda bloqueado. */
    val isSaving: Boolean = false
) {
    /** Muestra el aviso "Completa los campos obligatorios para guardar." */
    val showErrorBanner: Boolean get() = errors.isNotEmpty()
}

/** Avisos de una sola vez que el Fragment atiende y luego olvida. */
sealed interface AddTrackEvent {

    /** El backend guardó la canción; el Fragment vuelve al detalle del álbum. */
    data class Added(val trackId: Int, val trackName: String) : AddTrackEvent

    /** No se pudo guardar; [message] se muestra al usuario. */
    data class Failed(val message: String) : AddTrackEvent
}
