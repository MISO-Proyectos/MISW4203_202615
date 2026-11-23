package co.edu.uniandes.misw4203.vinilos.ui.albums.comments

/** Campos del formulario "Comentar álbum" (HU09). */
enum class CommentFormField { COLLECTOR, RATING, DESCRIPTION }

/** Problemas que puede tener un campo; el Fragment elige el texto. */
enum class CommentFormError { REQUIRED, TOO_LONG }

/**
 * Valida el formulario "Comentar álbum". No usa clases de Android para poder
 * probarse con pruebas unitarias.
 */
object CommentFormValidator {

    const val MAX_DESCRIPTION_LENGTH = 500
    const val MIN_RATING = 1
    const val MAX_RATING = 5

    /**
     * Devuelve los campos con error; un mapa vacío significa que el formulario es válido.
     * La calificación debe estar entre 1 y 5 (el backend acepta también 0, la app no).
     */
    fun validate(
        collectorId: Int?,
        rating: Int,
        description: String
    ): Map<CommentFormField, CommentFormError> = buildMap {
        if (collectorId == null) put(CommentFormField.COLLECTOR, CommentFormError.REQUIRED)
        if (rating !in MIN_RATING..MAX_RATING) put(CommentFormField.RATING, CommentFormError.REQUIRED)
        val text = description.trim()
        when {
            text.isEmpty() -> put(CommentFormField.DESCRIPTION, CommentFormError.REQUIRED)
            text.length > MAX_DESCRIPTION_LENGTH -> put(CommentFormField.DESCRIPTION, CommentFormError.TOO_LONG)
        }
    }
}
