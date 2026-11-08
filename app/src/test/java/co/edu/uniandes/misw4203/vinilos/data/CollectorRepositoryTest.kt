package co.edu.uniandes.misw4203.vinilos.data

import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.collectorDto
import co.edu.uniandes.misw4203.vinilos.data.adapter.CollectorServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.repository.CollectorRepository
import co.edu.uniandes.misw4203.vinilos.data.repository.CollectorRepositoryException
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class CollectorRepositoryTest {

    private fun repositoryWith(api: FakeBackendApi) =
        CollectorRepository(CollectorServiceAdapter(api))

    @Test
    fun `devuelve los coleccionistas cuando el backend responde`() = runBlocking {
        val result = repositoryWith(
            FakeBackendApi(collectors = listOf(collectorDto(id = 100), collectorDto(id = 101)))
        ).getCollectors()

        assertTrue(result.isSuccess)
        assertEquals(listOf(100, 101), result.getOrThrow().map { it.id })
    }

    @Test
    fun `descarta los coleccionistas que llegan sin id`() = runBlocking {
        val result = repositoryWith(
            FakeBackendApi(collectors = listOf(collectorDto(id = null), collectorDto(id = 101)))
        ).getCollectors()

        assertEquals(listOf(101), result.getOrThrow().map { it.id })
    }

    @Test
    fun `una lista vacia es un resultado exitoso`() = runBlocking {
        val result = repositoryWith(FakeBackendApi(collectors = emptyList())).getCollectors()

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
    }

    @Test
    fun `sin conexion devuelve NoConnection`() = runBlocking {
        val result = repositoryWith(FakeBackendApi(error = IOException("timeout"))).getCollectors()

        assertTrue(result.exceptionOrNull() is CollectorRepositoryException.NoConnection)
    }

    @Test
    fun `un error HTTP devuelve Server con el codigo`() = runBlocking {
        val httpError = HttpException(Response.error<Any>(500, "".toResponseBody(null)))

        val error = repositoryWith(FakeBackendApi(error = httpError)).getCollectors().exceptionOrNull()

        assertTrue(error is CollectorRepositoryException.Server)
        assertEquals(500, (error as CollectorRepositoryException.Server).code)
    }

    @Test
    fun `un JSON invalido devuelve Unexpected`() = runBlocking {
        val result = repositoryWith(FakeBackendApi(error = JsonSyntaxException("bad json"))).getCollectors()

        assertTrue(result.exceptionOrNull() is CollectorRepositoryException.Unexpected)
    }

    @Test
    fun `los errores traen un mensaje entendible para el usuario`() = runBlocking {
        val error = repositoryWith(FakeBackendApi(error = IOException())).getCollectors().exceptionOrNull()

        assertTrue(error?.message.orEmpty().contains("conexión"))
    }
}
