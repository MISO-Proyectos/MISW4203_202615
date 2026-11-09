package co.edu.uniandes.misw4203.vinilos.data.dto

/**
 * Copia del JSON que devuelve el backend en GET /collectors y GET /collectors/{id}.
 * El listado (HU05) solo trae los cuatro primeros campos; comentarios y artistas favoritos
 * llegan en el detalle (HU06) y por eso valen null por defecto. Gson ignora lo que no esté
 * declarado (por ejemplo collectorAlbums, que no trae el nombre del álbum: ver CollectorAlbumDto).
 * Los campos son anulables porque Gson no respeta la nulabilidad de Kotlin:
 * si el backend omite un campo, el adapter pone un valor por defecto.
 */
data class CollectorDto(
    val id: Int?,
    val name: String?,
    val telephone: String?,
    val email: String?,
    val comments: List<CommentDto>? = null,
    val favoritePerformers: List<PerformerDto>? = null
)
