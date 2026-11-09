package co.edu.uniandes.misw4203.vinilos.data.repository

import co.edu.uniandes.misw4203.vinilos.data.adapter.CollectorServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.model.Collector
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

/**
 * Único punto al que los ViewModel piden coleccionistas. Nunca lanza excepciones:
 * devuelve Result.success con los datos o Result.failure con un CollectorRepositoryException.
 */
class CollectorRepository(
    private val serviceAdapter: CollectorServiceAdapter
) {

    /** HU05: listado de coleccionistas. Una lista vacía es un resultado válido (estado Empty). */
    suspend fun getCollectors(): Result<List<Collector>> =
        safeCall { serviceAdapter.getCollectors() }

    /** HU06: detalle de un coleccionista. Si no existe devuelve NotFound. */
    suspend fun getCollector(id: Int): Result<Collector> =
        safeCall { serviceAdapter.getCollector(id) }

    /** Ejecuta la llamada y traduce los errores técnicos a CollectorRepositoryException. */
    private suspend fun <T> safeCall(call: suspend () -> T): Result<T> =
        try {
            Result.success(call())
        } catch (e: CancellationException) {
            // La corrutina se canceló (por ejemplo, se cerró la pantalla): no es un error
            throw e
        } catch (e: IOException) {
            Result.failure(CollectorRepositoryException.NoConnection(e))
        } catch (e: HttpException) {
            val error = if (e.code() == HTTP_NOT_FOUND) {
                CollectorRepositoryException.NotFound(e)
            } else {
                CollectorRepositoryException.Server(e.code(), e)
            }
            Result.failure(error)
        } catch (e: Exception) {
            Result.failure(CollectorRepositoryException.Unexpected(e))
        }

    private companion object {
        const val HTTP_NOT_FOUND = 404
    }
}
