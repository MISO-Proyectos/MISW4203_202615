package co.edu.uniandes.misw4203.vinilos.data.repository

import co.edu.uniandes.misw4203.vinilos.data.adapter.ArtistServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.model.Artist
import co.edu.uniandes.misw4203.vinilos.data.model.ArtistDetail
import co.edu.uniandes.misw4203.vinilos.data.model.ArtistType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Único punto al que los ViewModel piden artistas. Nunca lanza excepciones:
 * devuelve Result.success con los datos o Result.failure con un RepositoryException.
 * Las llamadas corren en [dispatcher] (por defecto Dispatchers.IO).
 */
class ArtistRepository(
    private val serviceAdapter: ArtistServiceAdapter,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /** Último listado cargado con éxito. Se reemplaza entero al recargar. */
    @Volatile
    private var cachedArtists: List<Artist>? = null

    /**
     * HU03: listado de artistas (músicos y bandas). Una lista vacía es un resultado válido
     * (estado Empty). Si ya hay un listado en memoria lo devuelve sin ir a la red, salvo que
     * [forceRefresh] sea true. Un fallo nunca se guarda.
     */
    suspend fun getArtists(forceRefresh: Boolean = false): Result<List<Artist>> {
        if (!forceRefresh) cachedArtists?.let { return Result.success(it) }
        return call { serviceAdapter.getArtists() }.onSuccess { cachedArtists = it }
    }

    /** HU04: detalle de un artista (músico o banda). No se cachea. */
    suspend fun getArtistDetail(id: Int, type: ArtistType): Result<ArtistDetail> =
        call { serviceAdapter.getArtistDetail(id, type) }

    private suspend fun <T> call(block: suspend () -> T): Result<T> =
        safeCall(
            dispatcher = dispatcher,
            notFoundMessage = "No encontramos este artista. Puede que haya sido eliminado.",
            unexpectedMessage = "Ocurrió un error inesperado al cargar los artistas.",
            call = block
        )
}
