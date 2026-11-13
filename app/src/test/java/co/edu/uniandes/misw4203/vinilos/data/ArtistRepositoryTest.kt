package co.edu.uniandes.misw4203.vinilos.data

import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.bandDto
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.musicianDto
import co.edu.uniandes.misw4203.vinilos.data.adapter.ArtistServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.repository.ArtistRepository
import co.edu.uniandes.misw4203.vinilos.data.repository.RepositoryException
import com.google.gson.JsonSyntaxException
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

@OptIn(ExperimentalCoroutinesApi::class)
class ArtistRepositoryTest {

    // Se ejecuta de inmediato en el hilo de la prueba: sin hilos reales ni esperas
    private val dispatcher = UnconfinedTestDispatcher()

    private fun repositoryWith(api: FakeBackendApi) =
        ArtistRepository(ArtistServiceAdapter(api, dispatcher), dispatcher)

    @Test
    fun `devuelve musicos y bandas cuando el backend responde`() = runBlocking {
        val result = repositoryWith(
            FakeBackendApi(musicians = listOf(musicianDto(id = 100)), bands = listOf(bandDto(id = 101)))
        ).getArtists()

        assertTrue(result.isSuccess)
        assertEquals(listOf(100, 101), result.getOrThrow().map { it.id })
    }

    @Test
    fun `una lista vacia es un resultado exitoso`() = runBlocking {
        val result = repositoryWith(FakeBackendApi()).getArtists()

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
    }

    @Test
    fun `sin conexion devuelve NoConnection`() = runBlocking {
        val result = repositoryWith(FakeBackendApi(error = IOException("timeout"))).getArtists()

        assertTrue(result.exceptionOrNull() is RepositoryException.NoConnection)
    }

    @Test
    fun `un error HTTP devuelve Server con el codigo`() = runBlocking {
        val httpError = HttpException(Response.error<Any>(500, "".toResponseBody(null)))

        val error = repositoryWith(FakeBackendApi(error = httpError)).getArtists().exceptionOrNull()

        assertTrue(error is RepositoryException.Server)
        assertEquals(500, (error as RepositoryException.Server).code)
    }

    @Test
    fun `si falla solo la ruta de bandas falla todo el listado`() = runBlocking {
        val result = repositoryWith(
            FakeBackendApi(musicians = listOf(musicianDto()), bandsError = IOException("timeout"))
        ).getArtists()

        assertTrue(result.exceptionOrNull() is RepositoryException.NoConnection)
    }

    @Test
    fun `un JSON invalido devuelve Unexpected`() = runBlocking {
        val result = repositoryWith(FakeBackendApi(error = JsonSyntaxException("bad json"))).getArtists()

        assertTrue(result.exceptionOrNull() is RepositoryException.Unexpected)
    }

    @Test
    fun `los errores traen un mensaje entendible para el usuario`() = runBlocking {
        val error = repositoryWith(FakeBackendApi(error = IOException())).getArtists().exceptionOrNull()

        assertTrue(error?.message.orEmpty().contains("conexión"))
    }

    @Test
    fun `un 404 devuelve NotFound (antes era Server)`() = runBlocking {
        val httpError = HttpException(Response.error<Any>(404, "".toResponseBody(null)))

        val error = repositoryWith(FakeBackendApi(error = httpError)).getArtists().exceptionOrNull()

        assertTrue(error is RepositoryException.NotFound)
        assertTrue(error?.message.orEmpty().contains("No encontramos este artista"))
    }

    @Test
    fun `un JSON invalido conserva el mensaje de artistas`() = runBlocking {
        val error = repositoryWith(FakeBackendApi(error = JsonSyntaxException("bad json"))).getArtists().exceptionOrNull()

        assertEquals("Ocurrió un error inesperado al cargar los artistas.", error?.message)
    }
}
