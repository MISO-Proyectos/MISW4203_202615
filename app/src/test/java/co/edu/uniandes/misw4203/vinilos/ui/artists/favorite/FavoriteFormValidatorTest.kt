package co.edu.uniandes.misw4203.vinilos.ui.artists.favorite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** HU10: validación del formulario "Agregar a favoritos". */
class FavoriteFormValidatorTest {

    @Test
    fun `sin coleccionista el campo es obligatorio`() {
        val errors = FavoriteFormValidator.validate(null)

        assertEquals(mapOf(FavoriteFormField.COLLECTOR to FavoriteFormError.REQUIRED), errors)
    }

    @Test
    fun `con coleccionista elegido el formulario es valido`() {
        assertTrue(FavoriteFormValidator.validate(100).isEmpty())
    }

    @Test
    fun `el id cero tambien cuenta como elegido`() {
        assertTrue(FavoriteFormValidator.validate(0).isEmpty())
    }
}
