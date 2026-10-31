package co.edu.uniandes.misw4203.vinilos.data

import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.albumDto
import co.edu.uniandes.misw4203.vinilos.data.adapter.AlbumServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.dto.PerformerDto
import co.edu.uniandes.misw4203.vinilos.data.model.Performer
import co.edu.uniandes.misw4203.vinilos.data.model.Track
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumServiceAdapterTest {

    @Test
    fun `convierte el JSON del backend en modelos`() = runBlocking {
        val adapter = AlbumServiceAdapter(FakeBackendApi(albums = listOf(albumDto())))

        val album = adapter.getAlbums().single()

        assertEquals(100, album.id)
        assertEquals("Buscando América", album.name)
        assertEquals(1984, album.releaseYear)
        assertEquals("Salsa", album.genre)
        assertEquals("Elektra", album.recordLabel)
        assertEquals(listOf(Performer(1, "Rubén Blades", "rb.jpg")), album.performers)
        assertEquals(listOf(Track(10, "Decisiones", "5:05")), album.tracks)
    }

    @Test
    fun `una lista vacia del backend da una lista vacia`() = runBlocking {
        val adapter = AlbumServiceAdapter(FakeBackendApi(albums = emptyList()))

        assertTrue(adapter.getAlbums().isEmpty())
    }

    @Test
    fun `descarta los albumes sin id`() = runBlocking {
        val adapter = AlbumServiceAdapter(
            FakeBackendApi(albums = listOf(albumDto(id = null), albumDto(id = 7)))
        )

        assertEquals(listOf(7), adapter.getAlbums().map { it.id })
    }

    @Test
    fun `los campos que faltan quedan con valores por defecto`() = runBlocking {
        val adapter = AlbumServiceAdapter(
            FakeBackendApi(
                albums = listOf(
                    albumDto(name = null, releaseDate = null, performers = null, tracks = null)
                )
            )
        )

        val album = adapter.getAlbums().single()

        assertEquals("", album.name)
        assertNull(album.releaseYear)
        assertTrue(album.performers.isEmpty())
        assertTrue(album.tracks.isEmpty())
    }

    @Test
    fun `performerNames une los interpretes con coma`() = runBlocking {
        val performers = listOf(PerformerDto(1, "Queen", ""), PerformerDto(2, "Freddie Mercury", ""))
        val adapter = AlbumServiceAdapter(FakeBackendApi(albums = listOf(albumDto(performers = performers))))

        assertEquals("Queen, Freddie Mercury", adapter.getAlbums().single().performerNames)
    }

    @Test
    fun `parseReleaseYear toma el anio de la fecha ISO`() {
        assertEquals(1975, AlbumServiceAdapter.parseReleaseYear("1975-11-21T00:00:00.000Z"))
        assertEquals(2002, AlbumServiceAdapter.parseReleaseYear("2002"))
        assertNull(AlbumServiceAdapter.parseReleaseYear("abc"))
        assertNull(AlbumServiceAdapter.parseReleaseYear(""))
        assertNull(AlbumServiceAdapter.parseReleaseYear(null))
    }
}
