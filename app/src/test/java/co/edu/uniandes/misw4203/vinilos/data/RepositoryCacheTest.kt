package co.edu.uniandes.misw4203.vinilos.data

import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.albumDto
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.collectorDto
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.musicianDto
import co.edu.uniandes.misw4203.vinilos.data.adapter.AlbumServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.adapter.ArtistServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.adapter.CollectorServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.repository.AlbumRepository
import co.edu.uniandes.misw4203.vinilos.data.repository.ArtistRepository
import co.edu.uniandes.misw4203.vinilos.data.repository.CollectorRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** Caché en memoria de los tres listados: carga inicial, forceRefresh y fallos. */
@OptIn(ExperimentalCoroutinesApi::class)
class RepositoryCacheTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private fun albums(api: FakeBackendApi) = AlbumRepository(AlbumServiceAdapter(api, dispatcher), dispatcher)
    private fun artists(api: FakeBackendApi) = ArtistRepository(ArtistServiceAdapter(api, dispatcher), dispatcher)
    private fun collectors(api: FakeBackendApi) =
        CollectorRepository(CollectorServiceAdapter(api, dispatcher), dispatcher)

    // Álbumes

    @Test
    fun `albumes - la segunda llamada no va a la red`() = runBlocking {
        val api = FakeBackendApi(albums = listOf(albumDto(id = 1)))
        val repository = albums(api)

        val first = repository.getAlbums().getOrThrow()
        val second = repository.getAlbums().getOrThrow()

        assertEquals(1, api.listRequests)
        assertEquals(first, second)
    }

    @Test
    fun `albumes - forceRefresh vuelve a la red y reemplaza la cache`() = runBlocking {
        val api = FakeBackendApi(albums = listOf(albumDto(id = 1)))
        val repository = albums(api)
        repository.getAlbums()

        api.albums = listOf(albumDto(id = 1), albumDto(id = 2))
        val refreshed = repository.getAlbums(forceRefresh = true).getOrThrow()
        val cached = repository.getAlbums().getOrThrow()

        assertEquals(2, api.listRequests)
        assertEquals(listOf(1, 2), refreshed.map { it.id })
        assertEquals(listOf(1, 2), cached.map { it.id })
    }

    @Test
    fun `albumes - un fallo no se guarda y la siguiente llamada reintenta`() = runBlocking {
        val api = FakeBackendApi(albums = listOf(albumDto(id = 1)), error = IOException("sin red"))
        val repository = albums(api)

        assertTrue(repository.getAlbums().isFailure)
        api.error = null
        val result = repository.getAlbums()

        assertTrue(result.isSuccess)
        assertEquals(2, api.listRequests)
    }

    @Test
    fun `albumes - un forceRefresh fallido conserva la cache anterior`() = runBlocking {
        val api = FakeBackendApi(albums = listOf(albumDto(id = 1)))
        val repository = albums(api)
        repository.getAlbums()

        api.error = IOException("sin red")
        assertTrue(repository.getAlbums(forceRefresh = true).isFailure)
        val cached = repository.getAlbums().getOrThrow()

        assertEquals(listOf(1), cached.map { it.id })
        assertEquals(2, api.listRequests)
    }

    @Test
    fun `albumes - el detalle no se cachea`() = runBlocking {
        val api = FakeBackendApi(albums = listOf(albumDto(id = 1)))
        val repository = albums(api)

        repository.getAlbum(1)
        api.albums = emptyList()

        assertTrue(repository.getAlbum(1).isFailure)
    }

    // Artistas

    @Test
    fun `artistas - la segunda llamada no va a la red y forceRefresh si`() = runBlocking {
        val api = FakeBackendApi(musicians = listOf(musicianDto(id = 100)))
        val repository = artists(api)

        repository.getArtists()
        repository.getArtists()
        assertEquals(1, api.listRequests)

        api.musicians = listOf(musicianDto(id = 100), musicianDto(id = 102))
        val refreshed = repository.getArtists(forceRefresh = true).getOrThrow()

        assertEquals(2, api.listRequests)
        assertEquals(listOf(100, 102), refreshed.map { it.id })
    }

    @Test
    fun `artistas - un fallo no se guarda`() = runBlocking {
        val api = FakeBackendApi(musicians = listOf(musicianDto()), error = IOException("sin red"))
        val repository = artists(api)

        assertTrue(repository.getArtists().isFailure)
        api.error = null

        assertTrue(repository.getArtists().isSuccess)
        assertEquals(2, api.listRequests)
    }

    // Coleccionistas

    @Test
    fun `coleccionistas - la segunda llamada no va a la red y forceRefresh si`() = runBlocking {
        val api = FakeBackendApi(collectors = listOf(collectorDto(id = 100)))
        val repository = collectors(api)

        repository.getCollectors()
        repository.getCollectors()
        assertEquals(1, api.listRequests)

        api.collectors = listOf(collectorDto(id = 100), collectorDto(id = 101))
        val refreshed = repository.getCollectors(forceRefresh = true).getOrThrow()

        assertEquals(2, api.listRequests)
        assertEquals(listOf(100, 101), refreshed.map { it.id })
    }

    @Test
    fun `coleccionistas - un fallo no se guarda`() = runBlocking {
        val api = FakeBackendApi(collectors = listOf(collectorDto()), error = IOException("sin red"))
        val repository = collectors(api)

        assertTrue(repository.getCollectors().isFailure)
        api.error = null

        assertTrue(repository.getCollectors().isSuccess)
        assertEquals(2, api.listRequests)
    }
}
