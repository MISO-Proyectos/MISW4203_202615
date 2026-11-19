package co.edu.uniandes.misw4203.vinilos.ui.artists.favorite

import co.edu.uniandes.misw4203.vinilos.data.model.ArtistType
import co.edu.uniandes.misw4203.vinilos.data.model.Collector

/**
 * Estado de la pantalla "Agregar a favoritos" (HU10). Los datos de la tarjeta del artista
 * llegan en los argumentos de navegación y no cambian; el resto es la lista de
 * coleccionistas, lo elegido y los errores.
 */
data class AddFavoriteArtistUiState(
    val artistName: String,
    val artistType: ArtistType,
    val albumCount: Int,
    val collectors: List<Collector> = emptyList(),
    val isLoadingCollectors: Boolean = true,
    /** true si no cargó la lista de coleccionistas: el campo lo dice y deja reintentar. */
    val collectorsLoadFailed: Boolean = false,
    val selectedCollectorId: Int? = null,
    /** Errores por campo; solo se llenan después de tocar "Agregar a favoritos". */
    val errors: Map<FavoriteFormField, FavoriteFormError> = emptyMap(),
    /** Nombre del coleccionista que ya tiene al artista; muestra ese error en el campo Coleccionista. */
    val duplicateCollectorName: String? = null,
    /** true mientras se envía al backend: el formulario queda bloqueado. */
    val isSaving: Boolean = false
) {
    /** Muestra el aviso "Completa los campos obligatorios para guardar." */
    val showErrorBanner: Boolean get() = errors.isNotEmpty()
}

/** Avisos de una sola vez que el Fragment atiende y luego olvida. */
sealed interface AddFavoriteArtistEvent {

    /** El backend guardó el favorito; el Fragment vuelve al detalle del artista. */
    data class Added(val artistName: String, val collectorName: String) : AddFavoriteArtistEvent

    /** No se pudo guardar; [message] se muestra al usuario. */
    data class Failed(val message: String) : AddFavoriteArtistEvent
}
