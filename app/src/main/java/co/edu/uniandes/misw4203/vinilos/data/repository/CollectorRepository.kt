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
            Result.failure(CollectorRepositoryException.Server(e.code(), e))
        } catch (e: Exception) {
            Result.failure(CollectorRepositoryException.Unexpected(e))
        }
}
