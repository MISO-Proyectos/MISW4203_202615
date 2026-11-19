package co.edu.uniandes.misw4203.vinilos.data

import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.albumDto
import co.edu.uniandes.misw4203.vinilos.data.adapter.AlbumServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.adapter.AlbumServiceAdapter.Companion.toDto
import co.edu.uniandes.misw4203.vinilos.data.dto.TrackCreateDto
import co.edu.uniandes.misw4203.vinilos.data.model.NewTrack
import co.edu.uniandes.misw4203.vinilos.data.model.Track
import co.edu.uniandes.misw4203.vinilos.data.repository.AlbumRepository
import co.edu.uniandes.misw4203.vinilos.data.repository.RepositoryException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/** HU08: asociar una canción a un álbum en el Service Adapter y en el repositorio. */
@OptIn(ExperimentalCoroutinesApi::class)
class AlbumAddTrackTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private val newTrack = NewTrack(name = "  Todos vuelven ", duration = " 05:03 ")

    private fun repositoryWith(api: FakeBackendApi) =
        AlbumRepository(AlbumServiceAdapter(api, dispatcher), dispatcher)

    @Test
    fun `el cuerpo lleva el nombre y la duracion sin espacios sobrantes`() {
        assertEquals(TrackCreateDto("Todos vuelven", "05:03"), newTrack.toDto())
    }

    @Test
    fun `el adapter envia la cancion al album y devuelve la creada con su id`() = runBlocking {
        val api = FakeBackendApi(albums = listOf(albumDto(id = 100)))

        val track = AlbumServiceAdapter(api, dispatcher).addTrack(100, newTrack)

        // albumDto ya trae la canción 10, así que la nueva recibe el 11
        assertEquals(Track(11, "Todos vuelven", "05:03"), track)
        assertEquals(listOf(100 to TrackCreateDto("Todos vuelven", "05:03")), api.addedTracks)
    }

    @Test
    fun `el repositorio devuelve la cancion creada`() = runBlocking {
        val result = repositoryWith(FakeBackendApi(albums = listOf(albumDto(id = 100))))
            .addTrack(100, newTrack)

        assertTrue(result.isSuccess)
        assertEquals("Todos vuelven", result.getOrThrow().name)
    }

    @Test
    fun `al volver a pedir el detalle ya trae la cancion nueva`() = runBlocking {
        val repository = repositoryWith(FakeBackendApi(albums = listOf(albumDto(id = 100))))

        repository.addTrack(100, newTrack)
        val album = repository.getAlbum(100).getOrThrow()

        assertEquals(listOf("Decisiones", "Todos vuelven"), album.tracks.map { it.name })
    }

    @Test
    fun `si el album no existe devuelve NotFound`() = runBlocking {
        val error = repositoryWith(FakeBackendApi(albums = emptyList()))
            .addTrack(999, newTrack)
            .exceptionOrNull()

        assertTrue(error is RepositoryException.NotFound)
    }

    @Test
    fun `sin conexion devuelve NoConnection`() = runBlocking {
        val error = repositoryWith(FakeBackendApi(error = IOException("timeout")))
            .addTrack(100, newTrack)
            .exceptionOrNull()

        assertTrue(error is RepositoryException.NoConnection)
    }

    @Test
    fun `si el backend rechaza la cancion devuelve Server con el codigo`() = runBlocking {
        val httpError = HttpException(Response.error<Any>(412, "".toResponseBody(null)))

        val error = repositoryWith(FakeBackendApi(error = httpError))
            .addTrack(100, newTrack)
            .exceptionOrNull()

        assertTrue(error is RepositoryException.Server)
        assertEquals(412, (error as RepositoryException.Server).code)
    }
}
