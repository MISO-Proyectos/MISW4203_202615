package co.edu.uniandes.misw4203.vinilos.data

import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.albumDto
import co.edu.uniandes.misw4203.vinilos.data.adapter.AlbumServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.repository.AlbumRepository
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
class AlbumRepositoryTest {

    // Se ejecuta de inmediato en el hilo de la prueba: sin hilos reales ni esperas
    private val dispatcher = UnconfinedTestDispatcher()

    private fun repositoryWith(api: FakeBackendApi) =
        AlbumRepository(AlbumServiceAdapter(api, dispatcher), dispatcher)

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

        assertTrue(result.exceptionOrNull() is RepositoryException.NoConnection)
    }

    @Test
    fun `un error HTTP devuelve Server con el codigo`() = runBlocking {
        val httpError = HttpException(Response.error<Any>(500, "".toResponseBody(null)))

        val error = repositoryWith(FakeBackendApi(error = httpError)).getAlbums().exceptionOrNull()

        assertTrue(error is RepositoryException.Server)
        assertEquals(500, (error as RepositoryException.Server).code)
    }

    @Test
    fun `un JSON invalido devuelve Unexpected`() = runBlocking {
        val result = repositoryWith(FakeBackendApi(error = JsonSyntaxException("bad json"))).getAlbums()

        assertTrue(result.exceptionOrNull() is RepositoryException.Unexpected)
    }

    @Test
    fun `los errores traen un mensaje entendible para el usuario`() = runBlocking {
        val error = repositoryWith(FakeBackendApi(error = IOException())).getAlbums().exceptionOrNull()

        assertTrue(error?.message.orEmpty().contains("conexión"))
    }

    @Test
    fun `getAlbum devuelve el album pedido`() = runBlocking {
        val result = repositoryWith(FakeBackendApi(albums = listOf(albumDto(id = 1), albumDto(id = 2))))
            .getAlbum(2)

        assertEquals(2, result.getOrThrow().id)
    }

    @Test
    fun `getAlbum de un album que no existe devuelve NotFound`() = runBlocking {
        val result = repositoryWith(FakeBackendApi(albums = emptyList())).getAlbum(999)

        assertTrue(result.exceptionOrNull() is RepositoryException.NotFound)
    }

    @Test
    fun `getAlbum sin conexion devuelve NoConnection`() = runBlocking {
        val result = repositoryWith(FakeBackendApi(error = IOException())).getAlbum(1)

        assertTrue(result.exceptionOrNull() is RepositoryException.NoConnection)
    }
}
