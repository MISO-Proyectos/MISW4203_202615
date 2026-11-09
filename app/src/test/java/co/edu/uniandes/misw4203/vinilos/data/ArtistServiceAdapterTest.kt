package co.edu.uniandes.misw4203.vinilos.data

import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.bandDto
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.musicianDto
import co.edu.uniandes.misw4203.vinilos.data.adapter.ArtistServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.adapter.ArtistServiceAdapter.Companion.toModel
import co.edu.uniandes.misw4203.vinilos.data.model.ArtistType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArtistServiceAdapterTest {

    @Test
    fun `convierte el JSON de un musico en un artista de tipo musico`() {
        val artist = musicianDto(id = 100, name = "Rubén Blades", image = "rb.jpg").toModel()

        assertEquals(100, artist?.id)
        assertEquals("Rubén Blades", artist?.name)
        assertEquals("rb.jpg", artist?.image)
        assertEquals(ArtistType.MUSICIAN, artist?.type)
    }

    @Test
    fun `convierte el JSON de una banda en un artista de tipo banda`() {
        val artist = bandDto(id = 101, name = "Queen", image = "queen.jpg").toModel()

        assertEquals(101, artist?.id)
        assertEquals("Queen", artist?.name)
        assertEquals("queen.jpg", artist?.image)
        assertEquals(ArtistType.BAND, artist?.type)
    }

    @Test
    fun `un artista sin id se descarta`() {
        assertNull(musicianDto(id = null).toModel())
        assertNull(bandDto(id = null).toModel())
    }

    @Test
    fun `los textos que no llegan quedan vacios en vez de null`() {
        val artist = bandDto(name = null, image = null).toModel()

        assertEquals("", artist?.name)
        assertEquals("", artist?.image)
    }

    @Test
    fun `getArtists une musicos y bandas ordenados por id`() = runBlocking {
        val adapter = ArtistServiceAdapter(
            FakeBackendApi(
                musicians = listOf(musicianDto(id = 102, name = "Miles Davis"), musicianDto(id = 100)),
                bands = listOf(bandDto(id = 103, name = "Soda Stereo"), bandDto(id = 101))
            )
        )

        val result = adapter.getArtists()

        assertEquals(listOf(100, 101, 102, 103), result.map { it.id })
        assertEquals(
            listOf(ArtistType.MUSICIAN, ArtistType.BAND, ArtistType.MUSICIAN, ArtistType.BAND),
            result.map { it.type }
        )
    }

    @Test
    fun `getArtists descarta los que no tienen id`() = runBlocking {
        val adapter = ArtistServiceAdapter(
            FakeBackendApi(
                musicians = listOf(musicianDto(id = null), musicianDto(id = 100)),
                bands = listOf(bandDto(id = null))
            )
        )

        assertEquals(listOf(100), adapter.getArtists().map { it.id })
    }
}
