package co.edu.uniandes.misw4203.vinilos.data.model

/**
 * Comentario que un coleccionista quiere publicar sobre un álbum (HU09).
 * Todavía no tiene id: el backend lo asigna al guardarlo.
 * [rating] ya viene validado (1 a 5) y [description] sin pasarse de 500 caracteres.
 */
data class NewComment(
    val collectorId: Int,
    val rating: Int,
    val description: String
)
