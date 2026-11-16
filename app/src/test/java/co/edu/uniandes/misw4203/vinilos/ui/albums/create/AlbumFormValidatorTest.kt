package co.edu.uniandes.misw4203.vinilos.ui.albums.create

import co.edu.uniandes.misw4203.vinilos.data.model.AlbumGenre
import co.edu.uniandes.misw4203.vinilos.data.model.RecordLabel
import co.edu.uniandes.misw4203.vinilos.data.model.ReleaseDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** HU07: reglas del formulario "Agregar álbum". */
class AlbumFormValidatorTest {

    private val today = ReleaseDate(2026, 11, 15)

    private fun validate(
        name: String = "Siembra",
        cover: String = "https://example.com/siembra.jpg",
        releaseDate: ReleaseDate? = ReleaseDate(1978, 8, 1),
        genre: AlbumGenre? = AlbumGenre.SALSA,
        recordLabel: RecordLabel? = RecordLabel.FANIA,
        description: String = "Un clásico de la salsa."
    ) = AlbumFormValidator.validate(name, cover, releaseDate, genre, recordLabel, description, today)

    @Test
    fun `un formulario completo no tiene errores`() {
        assertTrue(validate().isEmpty())
    }

    @Test
    fun `un formulario vacio marca todos los campos como obligatorios`() {
        val errors = validate(
            name = "",
            cover = "",
            releaseDate = null,
            genre = null,
            recordLabel = null,
            description = ""
        )

        assertEquals(AlbumFormField.entries.toSet(), errors.keys)
        assertTrue(errors.values.all { it == AlbumFormError.REQUIRED })
    }

    @Test
    fun `los campos con solo espacios cuentan como vacios`() {
        val errors = validate(name = "   ", cover = "  ", description = "\n ")

        assertEquals(AlbumFormError.REQUIRED, errors[AlbumFormField.NAME])
        assertEquals(AlbumFormError.REQUIRED, errors[AlbumFormField.COVER])
        assertEquals(AlbumFormError.REQUIRED, errors[AlbumFormField.DESCRIPTION])
    }

    @Test
    fun `una portada que no es un enlace da INVALID_URL`() {
        listOf("portada.jpg", "ftp://example.com/a.jpg", "https://", "https://sin-dominio", "https://a b.com/x.jpg")
            .forEach { cover ->
                assertEquals(cover, AlbumFormError.INVALID_URL, validate(cover = cover)[AlbumFormField.COVER])
            }
    }

    @Test
    fun `acepta enlaces http y https`() {
        assertTrue(AlbumFormValidator.isValidUrl("http://example.com/a.jpg"))
        assertTrue(AlbumFormValidator.isValidUrl("HTTPS://i.pinimg.com/564x/aa/bb.jpg?x=1"))
        assertTrue(AlbumFormValidator.isValidUrl("  https://example.com  "))
        assertFalse(AlbumFormValidator.isValidUrl("example.com/a.jpg"))
    }

    @Test
    fun `una fecha futura da FUTURE_DATE`() {
        assertEquals(
            AlbumFormError.FUTURE_DATE,
            validate(releaseDate = ReleaseDate(2026, 11, 16))[AlbumFormField.RELEASE_DATE]
        )
        assertEquals(
            AlbumFormError.FUTURE_DATE,
            validate(releaseDate = ReleaseDate(2027, 1, 1))[AlbumFormField.RELEASE_DATE]
        )
    }

    @Test
    fun `la fecha de hoy es valida`() {
        assertTrue(validate(releaseDate = today).isEmpty())
    }
}
