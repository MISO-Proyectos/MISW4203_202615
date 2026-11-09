package co.edu.uniandes.misw4203.vinilos.data.dto

/**
 * Copia del JSON que devuelve el backend en GET /musicians y GET /musicians/{id}.
 * Los campos son anulables porque Gson no respeta la nulabilidad de Kotlin.
 */
data class MusicianDto(
    val id: Int?,
    val name: String?,
    val image: String?,
    val description: String? = null,
    val birthDate: String? = null,
    val albums: List<AlbumDto>? = null
)
