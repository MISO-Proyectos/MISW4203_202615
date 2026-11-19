package co.edu.uniandes.misw4203.vinilos.data

import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.albumDto
import co.edu.uniandes.misw4203.vinilos.data.adapter.AlbumServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.adapter.AlbumServiceAdapter.Companion.formatReleaseDate
import co.edu.uniandes.misw4203.vinilos.data.adapter.AlbumServiceAdapter.Companion.toDto
import co.edu.uniandes.misw4203.vinilos.data.dto.AlbumCreateDto
import co.edu.uniandes.misw4203.vinilos.data.model.AlbumGenre
import co.edu.uniandes.misw4203.vinilos.data.model.NewAlbum
import co.edu.uniandes.misw4203.vinilos.data.model.RecordLabel
import co.edu.uniandes.misw4203.vinilos.data.model.ReleaseDate
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

/** HU07: creación de un álbum en el Service Adapter y en el repositorio. */
@OptIn(ExperimentalCoroutinesApi::class)
class AlbumCreateTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private val newAlbum = NewAlbum(
        name = "  Siembra  ",
        cover = " https://example.com/siembra.jpg ",
        releaseDate = ReleaseDate(1978, 8, 1),
        genre = AlbumGenre.SALSA,
        recordLabel = RecordLabel.FANIA,
        description = " Álbum de Willie Colón y Rubén Blades. "
    )

    private fun repositoryWith(api: FakeBackendApi) =
        AlbumRepository(AlbumServiceAdapter(api, dispatcher), dispatcher)

    @Test
    fun `la fecha se envia en ISO 8601 a medianoche UTC`() {
        assertEquals("1978-08-01T00:00:00.000Z", formatReleaseDate(ReleaseDate(1978, 8, 1)))
        assertEquals("2002-12-31T00:00:00.000Z", formatReleaseDate(ReleaseDate(2002, 12, 31)))
    }

    @Test
    fun `si la fecha no tiene mes ni dia se usa el primero de enero`() {
        assertEquals("1984-01-01T00:00:00.000Z", formatReleaseDate(ReleaseDate(1984)))
    }

    @Test
    fun `el cuerpo usa los valores del backend y quita espacios sobrantes`() {
        assertEquals(
            AlbumCreateDto(
                name = "Siembra",
                cover = "https://example.com/siembra.jpg",
                releaseDate = "1978-08-01T00:00:00.000Z",
                description = "Álbum de Willie Colón y Rubén Blades.",
                genre = "Salsa",
                recordLabel = "Fania Records"
            ),
            newAlbum.toDto()
        )
    }

    @Test
    fun `los generos y sellos se envian con el texto exacto del backend`() {
        assertEquals(
            listOf("Classical", "Salsa", "Rock", "Folk"),
            AlbumGenre.entries.map { it.apiValue }
        )
        assertEquals(
            listOf("Sony Music", "EMI", "Discos Fuentes", "Elektra", "Fania Records"),
            RecordLabel.entries.map { it.apiValue }
        )
    }

    @Test
    fun `el adapter envia el album y devuelve el creado con su id`() = runBlocking {
        val api = FakeBackendApi(albums = listOf(albumDto(id = 100)))

        val created = AlbumServiceAdapter(api, dispatcher).createAlbum(newAlbum)

        assertEquals(101, created.id)
        assertEquals("Siembra", created.name)
        assertEquals("Salsa", created.genre)
        assertEquals("Fania Records", created.recordLabel)
        assertEquals(ReleaseDate(1978, 8, 1), created.releaseDate)
        assertTrue(created.performers.isEmpty())
        assertEquals(listOf(newAlbum.toDto()), api.createdAlbums)
    }

    @Test
    fun `el repositorio devuelve el album creado`() = runBlocking {
        val result = repositoryWith(FakeBackendApi()).createAlbum(newAlbum)

        assertTrue(result.isSuccess)
        assertEquals("Siembra", result.getOrThrow().name)
    }

    @Test
    fun `despues de crear un album el catalogo se vuelve a pedir a la red`() = runBlocking {
        val api = FakeBackendApi(albums = listOf(albumDto(id = 100)))
        val repository = repositoryWith(api)
        repository.getAlbums()
        assertEquals(1, api.listRequests)

        repository.createAlbum(newAlbum)
        val albums = repository.getAlbums().getOrThrow()

        assertEquals(2, api.listRequests)
        assertEquals(listOf(100, 101), albums.map { it.id })
    }

    @Test
    fun `si falla la creacion el catalogo en memoria se conserva`() = runBlocking {
        val api = FakeBackendApi(albums = listOf(albumDto(id = 100)))
        val repository = repositoryWith(api)
        repository.getAlbums()

        api.error = IOException("sin red")
        repository.createAlbum(newAlbum)
        api.error = null
        repository.getAlbums()

        assertEquals(1, api.listRequests)
    }

    @Test
    fun `sin conexion la creacion devuelve NoConnection`() = runBlocking {
        val result = repositoryWith(FakeBackendApi(error = IOException("timeout"))).createAlbum(newAlbum)

        assertTrue(result.exceptionOrNull() is RepositoryException.NoConnection)
    }

    @Test
    fun `si el backend rechaza el album devuelve Server con el codigo`() = runBlocking {
        val httpError = HttpException(Response.error<Any>(412, "".toResponseBody(null)))

        val error = repositoryWith(FakeBackendApi(error = httpError)).createAlbum(newAlbum).exceptionOrNull()

        assertTrue(error is RepositoryException.Server)
        assertEquals(412, (error as RepositoryException.Server).code)
    }
}
