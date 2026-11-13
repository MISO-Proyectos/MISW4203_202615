package co.edu.uniandes.misw4203.vinilos.data.repository

import co.edu.uniandes.misw4203.vinilos.data.adapter.CollectorServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.model.Collector
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Único punto al que los ViewModel piden coleccionistas. Nunca lanza excepciones:
 * devuelve Result.success con los datos o Result.failure con un RepositoryException.
 * Las llamadas corren en [dispatcher] (por defecto Dispatchers.IO).
 */
class CollectorRepository(
    private val serviceAdapter: CollectorServiceAdapter,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /** Último listado cargado con éxito. Se reemplaza entero al recargar. */
    @Volatile
    private var cachedCollectors: List<Collector>? = null

    /**
     * HU05: listado de coleccionistas. Una lista vacía es un resultado válido (estado Empty).
     * Si ya hay un listado en memoria lo devuelve sin ir a la red, salvo que
     * [forceRefresh] sea true. Un fallo nunca se guarda.
     */
    suspend fun getCollectors(forceRefresh: Boolean = false): Result<List<Collector>> {
        if (!forceRefresh) cachedCollectors?.let { return Result.success(it) }
        return call { serviceAdapter.getCollectors() }.onSuccess { cachedCollectors = it }
    }

    /** HU06: detalle de un coleccionista (no se cachea). Si no existe devuelve NotFound. */
    suspend fun getCollector(id: Int): Result<Collector> =
        call { serviceAdapter.getCollector(id) }

    private suspend fun <T> call(block: suspend () -> T): Result<T> =
        safeCall(
            dispatcher = dispatcher,
            notFoundMessage = "No encontramos este coleccionista. Puede que haya sido eliminado.",
            unexpectedMessage = "Ocurrió un error inesperado al cargar los coleccionistas.",
            call = block
        )
}
