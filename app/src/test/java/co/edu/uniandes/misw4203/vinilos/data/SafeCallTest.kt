package co.edu.uniandes.misw4203.vinilos.data

import co.edu.uniandes.misw4203.vinilos.data.repository.RepositoryException
import co.edu.uniandes.misw4203.vinilos.data.repository.safeCall
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SafeCallTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private suspend fun <T> translate(call: suspend () -> T): Result<T> =
        safeCall(dispatcher, "no existe", "inesperado", call)

    private fun http(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody(null)))

    @Test
    fun `devuelve success con el valor cuando la llamada funciona`() = runBlocking {
        assertEquals(42, translate { 42 }.getOrThrow())
    }

    @Test
    fun `IOException se traduce a NoConnection`() = runBlocking {
        val cause = IOException("timeout")

        val error = translate<Int> { throw cause }.exceptionOrNull()

        assertTrue(error is RepositoryException.NoConnection)
        assertSame(cause, error?.cause)
    }

    @Test
    fun `HTTP 404 se traduce a NotFound con el mensaje recibido`() = runBlocking {
        val error = translate<Int> { throw http(404) }.exceptionOrNull()

        assertTrue(error is RepositoryException.NotFound)
        assertEquals("no existe", error?.message)
    }

    @Test
    fun `otro codigo HTTP se traduce a Server con el codigo`() = runBlocking {
        val error = translate<Int> { throw http(500) }.exceptionOrNull()

        assertTrue(error is RepositoryException.Server)
        assertEquals(500, (error as RepositoryException.Server).code)
    }

    @Test
    fun `cualquier otro error se traduce a Unexpected con el mensaje recibido`() = runBlocking {
        val error = translate<Int> { throw JsonSyntaxException("bad json") }.exceptionOrNull()

        assertTrue(error is RepositoryException.Unexpected)
        assertEquals("inesperado", error?.message)
    }

    @Test
    fun `la cancelacion se vuelve a lanzar y no se convierte en error`() {
        try {
            runBlocking { translate<Int> { throw CancellationException("cancelada") } }
            fail("Debía lanzar CancellationException")
        } catch (e: CancellationException) {
            assertEquals("cancelada", e.message)
        }
    }

    @Test
    fun `la llamada corre en el dispatcher recibido`() = runBlocking {
        val name = "hilo-de-prueba"
        val namedDispatcher = kotlinx.coroutines.newSingleThreadContext(name)
        try {
            val threadName = safeCall(namedDispatcher, "", "") { Thread.currentThread().name }.getOrThrow()
            assertTrue(threadName.startsWith(name))
        } finally {
            namedDispatcher.close()
        }
    }
}
