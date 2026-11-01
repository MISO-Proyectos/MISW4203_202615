package co.edu.uniandes.misw4203.vinilos.ui.albums.detail

import co.edu.uniandes.misw4203.vinilos.data.model.ReleaseDate
import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumDetailFormatterTest {

    private val months = listOf(
        "ene", "feb", "mar", "abr", "may", "jun",
        "jul", "ago", "sep", "oct", "nov", "dic"
    )

    @Test
    fun `la fecha completa se muestra como en el prototipo`() {
        assertEquals("1 ago 1984", AlbumDetailFormatter.formatReleaseDate(ReleaseDate(1984, 8, 1), months))
    }

    @Test
    fun `si faltan partes de la fecha se muestra lo que hay`() {
        assertEquals("nov 1975", AlbumDetailFormatter.formatReleaseDate(ReleaseDate(1975, 11), months))
        assertEquals("2002", AlbumDetailFormatter.formatReleaseDate(ReleaseDate(2002), months))
    }

    @Test
    fun `el numero de la cancion lleva dos cifras`() {
        assertEquals("01", AlbumDetailFormatter.formatTrackNumber(1))
        assertEquals("12", AlbumDetailFormatter.formatTrackNumber(12))
    }

    @Test
    fun `la duracion lleva minutos de dos cifras`() {
        assertEquals("05:05", AlbumDetailFormatter.formatDuration("5:05"))
        assertEquals("12:30", AlbumDetailFormatter.formatDuration("12:30"))
    }

    @Test
    fun `una duracion con otro formato se muestra tal como llego`() {
        assertEquals("3 min", AlbumDetailFormatter.formatDuration("3 min"))
        assertEquals("", AlbumDetailFormatter.formatDuration(""))
    }
}
