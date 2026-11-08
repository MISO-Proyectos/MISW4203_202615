package co.edu.uniandes.misw4203.vinilos.data

import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.bandDto
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.musicianDto
import co.edu.uniandes.misw4203.vinilos.data.adapter.ArtistServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.repository.ArtistRepository
import co.edu.uniandes.misw4203.vinilos.data.repository.ArtistRepositoryException
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ArtistRepositoryTest {

    private fun repositoryWith(api: FakeBackendApi) =
        ArtistRepository(ArtistServiceAdapter(api))

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

        assertTrue(result.exceptionOrNull() is ArtistRepositoryException.NoConnection)
    }

    @Test
    fun `un error HTTP devuelve Server con el codigo`() = runBlocking {
        val httpError = HttpException(Response.error<Any>(500, "".toResponseBody(null)))

        val error = repositoryWith(FakeBackendApi(error = httpError)).getArtists().exceptionOrNull()

        assertTrue(error is ArtistRepositoryException.Server)
        assertEquals(500, (error as ArtistRepositoryException.Server).code)
    }

    @Test
    fun `si falla solo la ruta de bandas falla todo el listado`() = runBlocking {
        val result = repositoryWith(
            FakeBackendApi(musicians = listOf(musicianDto()), bandsError = IOException("timeout"))
        ).getArtists()

        assertTrue(result.exceptionOrNull() is ArtistRepositoryException.NoConnection)
    }

    @Test
    fun `un JSON invalido devuelve Unexpected`() = runBlocking {
        val result = repositoryWith(FakeBackendApi(error = JsonSyntaxException("bad json"))).getArtists()

        assertTrue(result.exceptionOrNull() is ArtistRepositoryException.Unexpected)
    }

    @Test
    fun `los errores traen un mensaje entendible para el usuario`() = runBlocking {
        val error = repositoryWith(FakeBackendApi(error = IOException())).getArtists().exceptionOrNull()

        assertTrue(error?.message.orEmpty().contains("conexión"))
    }
}
