package co.edu.uniandes.misw4203.vinilos.ui.albums.comments

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** HU09: validación del formulario "Comentar álbum". */
class CommentFormValidatorTest {

    private fun errors(collectorId: Int? = 100, rating: Int = 4, description: String = "Muy bueno") =
        CommentFormValidator.validate(collectorId, rating, description)

    @Test
    fun `un formulario completo es valido`() {
        assertTrue(errors().isEmpty())
    }

    @Test
    fun `todo vacio marca los tres campos`() {
        assertEquals(
            mapOf(
                CommentFormField.COLLECTOR to CommentFormError.REQUIRED,
                CommentFormField.RATING to CommentFormError.REQUIRED,
                CommentFormField.DESCRIPTION to CommentFormError.REQUIRED
            ),
            errors(collectorId = null, rating = 0, description = "")
        )
    }

    @Test
    fun `sin coleccionista el campo es obligatorio`() {
        assertEquals(mapOf(CommentFormField.COLLECTOR to CommentFormError.REQUIRED), errors(collectorId = null))
    }

    @Test
    fun `la calificacion cero o mayor a cinco es obligatoria`() {
        assertEquals(mapOf(CommentFormField.RATING to CommentFormError.REQUIRED), errors(rating = 0))
        assertEquals(mapOf(CommentFormField.RATING to CommentFormError.REQUIRED), errors(rating = 6))
    }

    @Test
    fun `las calificaciones de uno a cinco son validas`() {
        (1..5).forEach { assertTrue(errors(rating = it).isEmpty()) }
    }

    @Test
    fun `un comentario solo con espacios es obligatorio`() {
        assertEquals(
            mapOf(CommentFormField.DESCRIPTION to CommentFormError.REQUIRED),
            errors(description = "   \n ")
        )
    }

    @Test
    fun `quinientos caracteres es valido y quinientos uno es demasiado largo`() {
        assertTrue(errors(description = "a".repeat(500)).isEmpty())
        assertEquals(
            mapOf(CommentFormField.DESCRIPTION to CommentFormError.TOO_LONG),
            errors(description = "a".repeat(501))
        )
    }

    @Test
    fun `los espacios de los extremos no cuentan para el limite`() {
        assertTrue(errors(description = " " + "a".repeat(500) + " ").isEmpty())
    }
}
