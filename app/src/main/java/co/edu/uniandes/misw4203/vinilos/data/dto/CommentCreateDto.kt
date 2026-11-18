package co.edu.uniandes.misw4203.vinilos.data.dto

/**
 * JSON que espera POST /albums/{albumId}/comments (HU09).
 */
data class CommentCreateDto(
    val description: String,
    val rating: Int,
    val collector: CollectorRefDto
)

data class CollectorRefDto(
    val id: Int
)
