package co.edu.uniandes.misw4203.vinilos.ui.albums.create

import co.edu.uniandes.misw4203.vinilos.data.model.ReleaseDate
import org.junit.Assert.assertEquals
import org.junit.Test

/** HU07: conversiones de fecha entre el calendario y el campo DD/MM/AAAA. */
class CreateAlbumFormatterTest {

    @Test
    fun `muestra la fecha como DD MM AAAA`() {
        assertEquals("01/08/1978", CreateAlbumFormatter.formatDate(ReleaseDate(1978, 8, 1)))
        assertEquals("31/12/2002", CreateAlbumFormatter.formatDate(ReleaseDate(2002, 12, 31)))
    }

    @Test
    fun `convierte los milisegundos del calendario en la fecha sin correrse de dia`() {
        // 1978-08-01T00:00:00Z
        assertEquals(ReleaseDate(1978, 8, 1), CreateAlbumFormatter.fromUtcMillis(270_777_600_000L))
    }

    @Test
    fun `ida y vuelta entre fecha y milisegundos da la misma fecha`() {
        listOf(ReleaseDate(1978, 8, 1), ReleaseDate(2000, 2, 29), ReleaseDate(2026, 11, 15)).forEach {
            assertEquals(it, CreateAlbumFormatter.fromUtcMillis(CreateAlbumFormatter.toUtcMillis(it)))
        }
    }

    @Test
    fun `toUtcMillis da la medianoche UTC`() {
        assertEquals(270_777_600_000L, CreateAlbumFormatter.toUtcMillis(ReleaseDate(1978, 8, 1)))
    }
}
