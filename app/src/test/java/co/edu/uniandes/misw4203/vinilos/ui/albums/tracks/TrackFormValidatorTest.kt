package co.edu.uniandes.misw4203.vinilos.ui.albums.tracks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** HU08: reglas del formulario "Agregar track". */
class TrackFormValidatorTest {

    @Test
    fun `un formulario completo no tiene errores`() {
        assertTrue(TrackFormValidator.validate("Todos vuelven", "05:03").isEmpty())
    }

    @Test
    fun `un formulario vacio marca el nombre obligatorio y la duracion con formato invalido`() {
        assertEquals(
            mapOf(
                TrackFormField.NAME to TrackFormError.REQUIRED,
                TrackFormField.DURATION to TrackFormError.INVALID_DURATION
            ),
            TrackFormValidator.validate("", "")
        )
    }

    @Test
    fun `un nombre con solo espacios cuenta como vacio`() {
        assertEquals(
            TrackFormError.REQUIRED,
            TrackFormValidator.validate("   ", "05:03")[TrackFormField.NAME]
        )
    }

    @Test
    fun `rechaza duraciones que no son MM SS`() {
        listOf("5", "503", "05:3", "05:60", "123:00", "05-03", "aa:bb", "05:03:00", ":30", "00:00")
            .forEach { duration ->
                assertEquals(
                    duration,
                    TrackFormError.INVALID_DURATION,
                    TrackFormValidator.validate("Track", duration)[TrackFormField.DURATION]
                )
            }
    }

    @Test
    fun `normaliza la duracion con minutos de dos cifras`() {
        assertEquals("05:03", TrackFormValidator.normalizeDuration("5:03"))
        assertEquals("05:03", TrackFormValidator.normalizeDuration(" 05:03 "))
        assertEquals("00:59", TrackFormValidator.normalizeDuration("0:59"))
        assertEquals("99:59", TrackFormValidator.normalizeDuration("99:59"))
    }

    @Test
    fun `una duracion de cero segundos no es valida`() {
        assertNull(TrackFormValidator.normalizeDuration("0:00"))
    }
}
