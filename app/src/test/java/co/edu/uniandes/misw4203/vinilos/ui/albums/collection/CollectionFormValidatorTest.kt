package co.edu.uniandes.misw4203.vinilos.ui.albums.collection

import co.edu.uniandes.misw4203.vinilos.data.model.CollectorAlbumStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** HU11: validación del formulario "Agregar a colección". */
class CollectionFormValidatorTest {

    private fun priceError(price: String) =
        CollectionFormValidator.validate(100, price, CollectorAlbumStatus.ACTIVE)[CollectionFormField.PRICE]

    @Test
    fun `todo vacio marca los tres campos`() {
        val errors = CollectionFormValidator.validate(null, "", null)

        assertEquals(
            mapOf(
                CollectionFormField.COLLECTOR to CollectionFormError.REQUIRED,
                CollectionFormField.PRICE to CollectionFormError.REQUIRED,
                CollectionFormField.STATUS to CollectionFormError.REQUIRED
            ),
            errors
        )
    }

    @Test
    fun `un precio solo con espacios es obligatorio`() {
        assertEquals(CollectionFormError.REQUIRED, priceError("   "))
    }

    @Test
    fun `el precio cero es invalido`() {
        assertEquals(CollectionFormError.INVALID_PRICE, priceError("0"))
        assertEquals(CollectionFormError.INVALID_PRICE, priceError("000"))
    }

    @Test
    fun `un precio negativo es invalido`() {
        assertEquals(CollectionFormError.INVALID_PRICE, priceError("-5"))
    }

    @Test
    fun `un precio con letras es invalido`() {
        assertEquals(CollectionFormError.INVALID_PRICE, priceError("12a"))
    }

    @Test
    fun `un precio con puntos o comas es invalido`() {
        assertEquals(CollectionFormError.INVALID_PRICE, priceError("35.000"))
        assertEquals(CollectionFormError.INVALID_PRICE, priceError("35,5"))
    }

    @Test
    fun `un precio de 10 digitos es invalido porque no cabe en un Int`() {
        assertEquals(CollectionFormError.INVALID_PRICE, priceError("1234567890"))
    }

    @Test
    fun `un precio de 9 digitos es valido`() {
        assertNull(priceError("999999999"))
    }

    @Test
    fun `un formulario completo es valido`() {
        assertTrue(CollectionFormValidator.validate(100, "35000", CollectorAlbumStatus.INACTIVE).isEmpty())
    }

    @Test
    fun `parsePrice acepta espacios alrededor`() {
        assertEquals(35000, CollectionFormValidator.parsePrice("  35000 "))
    }

    @Test
    fun `parsePrice devuelve null si no es valido`() {
        assertNull(CollectionFormValidator.parsePrice(""))
        assertNull(CollectionFormValidator.parsePrice("0"))
        assertNull(CollectionFormValidator.parsePrice("1 000"))
    }
}
