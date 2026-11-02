package co.edu.uniandes.misw4203.vinilos.data.model

/** Comentario de un álbum. [rating] va de 0 a 5 estrellas. */
data class Comment(
    val id: Int,
    val description: String,
    val rating: Int
)
