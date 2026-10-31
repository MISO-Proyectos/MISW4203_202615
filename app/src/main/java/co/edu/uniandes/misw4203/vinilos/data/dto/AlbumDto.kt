package co.edu.uniandes.misw4203.vinilos.data.dto

/**
 * Copia del JSON que devuelve el backend en GET /albums y GET /albums/{id}.
 * Los campos son anulables porque Gson no respeta la nulabilidad de Kotlin:
 * si el backend omite un campo, el adapter pone un valor por defecto.
 */
data class AlbumDto(
    val id: Int?,
    val name: String?,
    val cover: String?,
    val releaseDate: String?,
    val description: String?,
    val genre: String?,
    val recordLabel: String?,
    val tracks: List<TrackDto>?,
    val performers: List<PerformerDto>?
)
