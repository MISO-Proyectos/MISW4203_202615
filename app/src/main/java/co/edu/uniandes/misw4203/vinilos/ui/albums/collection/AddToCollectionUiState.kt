package co.edu.uniandes.misw4203.vinilos.ui.albums.collection

import co.edu.uniandes.misw4203.vinilos.data.model.Album
import co.edu.uniandes.misw4203.vinilos.data.model.Collector
import co.edu.uniandes.misw4203.vinilos.data.model.CollectorAlbumStatus

/**
 * Estado de la pantalla "Agregar a colección" (HU11). El precio lo guarda su campo
 * (y el ViewModel en SavedStateHandle); aquí van el álbum de la tarjeta, la lista de
 * coleccionistas, lo elegido y los errores.
 */
data class AddToCollectionUiState(
    /** Álbum de la tarjeta superior; null mientras carga o si no se pudo cargar. */
    val album: Album? = null,
    val collectors: List<Collector> = emptyList(),
    val isLoadingCollectors: Boolean = true,
    /** true si no cargó la lista de coleccionistas: el campo lo dice y deja reintentar. */
    val collectorsLoadFailed: Boolean = false,
    val selectedCollectorId: Int? = null,
    val status: CollectorAlbumStatus? = null,
    /** Errores por campo; solo se llenan después de tocar "Agregar a la colección". */
    val errors: Map<CollectionFormField, CollectionFormError> = emptyMap(),
    /** Nombre del coleccionista que ya tiene el álbum; muestra ese error en el campo Coleccionista. */
    val duplicateCollectorName: String? = null,
    /** true mientras se envía al backend: el formulario queda bloqueado. */
    val isSaving: Boolean = false
) {
    /** Muestra el aviso "Completa los campos obligatorios para guardar." */
    val showErrorBanner: Boolean get() = errors.isNotEmpty()
}

/** Avisos de una sola vez que el Fragment atiende y luego olvida. */
sealed interface AddToCollectionEvent {

    /** El backend guardó la relación; el Fragment vuelve al detalle del álbum. */
    data class Added(val albumName: String, val collectorName: String) : AddToCollectionEvent

    /** No se pudo guardar; [message] se muestra al usuario. */
    data class Failed(val message: String) : AddToCollectionEvent
}
