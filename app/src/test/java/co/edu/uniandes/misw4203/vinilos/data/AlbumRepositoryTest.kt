package co.edu.uniandes.misw4203.vinilos.data

import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.albumDto
import co.edu.uniandes.misw4203.vinilos.data.adapter.AlbumServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.repository.AlbumRepository
import co.edu.uniandes.misw4203.vinilos.data.repository.AlbumRepositoryException
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class AlbumRepositoryTest {

    private fun repositoryWith(api: FakeBackendApi) =
        AlbumRepository(AlbumServiceAdapter(api))

    @Test
    fun `devuelve los albumes cuando el backend responde`() = runBlocking {
        val result = repositoryWith(FakeBackendApi(albums = listOf(albumDto(id = 1), albumDto(id = 2))))
            .getAlbums()

        assertTrue(result.isSuccess)
        assertEquals(listOf(1, 2), result.getOrThrow().map { it.id })
    }

    @Test
    fun `una lista vacia es un resultado exitoso`() = runBlocking {
        val result = repositoryWith(FakeBackendApi(albums = emptyList())).getAlbums()

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
    }

    @Test
    fun `sin conexion devuelve NoConnection`() = runBlocking {
        val result = repositoryWith(FakeBackendApi(error = IOException("timeout"))).getAlbums()

        assertTrue(result.exceptionOrNull() is AlbumRepositoryException.NoConnection)
    }

    @Test
    fun `un error HTTP devuelve Server con el codigo`() = runBlocking {
        val httpError = HttpException(Response.error<Any>(500, "".toResponseBody(null)))

        val error = repositoryWith(FakeBackendApi(error = httpError)).getAlbums().exceptionOrNull()

        assertTrue(error is AlbumRepositoryException.Server)
        assertEquals(500, (error as AlbumRepositoryException.Server).code)
    }

    @Test
    fun `un JSON invalido devuelve Unexpected`() = runBlocking {
        val result = repositoryWith(FakeBackendApi(error = JsonSyntaxException("bad json"))).getAlbums()

        assertTrue(result.exceptionOrNull() is AlbumRepositoryException.Unexpected)
    }

    @Test
    fun `los errores traen un mensaje entendible para el usuario`() = runBlocking {
        val error = repositoryWith(FakeBackendApi(error = IOException())).getAlbums().exceptionOrNull()

        assertTrue(error?.message.orEmpty().contains("conexión"))
    }
}
