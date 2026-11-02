package co.edu.uniandes.misw4203.vinilos.data.dto

/** Comentario de un álbum, tal como llega en el JSON de GET /albums/{id}. */
data class CommentDto(
    val id: Int?,
    val description: String?,
    val rating: Int?
)
