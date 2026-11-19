package co.edu.uniandes.misw4203.vinilos.data.repository

import co.edu.uniandes.misw4203.vinilos.data.adapter.CollectorServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.model.ArtistType
import co.edu.uniandes.misw4203.vinilos.data.model.CollectionResult
import co.edu.uniandes.misw4203.vinilos.data.model.Collector
import co.edu.uniandes.misw4203.vinilos.data.model.CollectorAlbumStatus
import co.edu.uniandes.misw4203.vinilos.data.model.FavoriteResult
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

    /** Borra la lista de coleccionistas guardada (se usa con poca memoria); la próxima consulta va a la red. */
    fun clearCache() {
        cachedCollectors = null
    }

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

    /**
     * HU11: agrega el álbum a la colección del coleccionista. El backend no impide repetir
     * un álbum, así que primero consulta la colección: si ya está devuelve
     * [CollectionResult.ALREADY_IN_COLLECTION] sin hacer el POST. Si el coleccionista o el
     * álbum no existen devuelve NotFound. No toca la caché del listado de coleccionistas.
     */
    suspend fun addAlbumToCollector(
        collectorId: Int,
        albumId: Int,
        price: Int,
        status: CollectorAlbumStatus
    ): Result<CollectionResult> = safeCall(
        dispatcher = dispatcher,
        notFoundMessage = "No encontramos el álbum o el coleccionista. Puede que hayan sido eliminados.",
        unexpectedMessage = "No pudimos agregar el álbum a la colección. Intenta de nuevo."
    ) {
        if (albumId in serviceAdapter.getCollectorAlbumIds(collectorId)) {
            CollectionResult.ALREADY_IN_COLLECTION
        } else {
            serviceAdapter.addAlbumToCollector(collectorId, albumId, price, status)
            CollectionResult.ADDED
        }
    }

    /**
     * HU10: agrega el artista a los favoritos del coleccionista. El backend no impide repetir
     * un favorito, así que primero consulta los favoritos del coleccionista: si el artista ya
     * está devuelve [FavoriteResult.ALREADY_FAVORITE] sin hacer el POST. Si el coleccionista o
     * el artista no existen devuelve NotFound. No toca la caché del listado de coleccionistas.
     */
    suspend fun addFavoriteArtist(
        collectorId: Int,
        artistId: Int,
        type: ArtistType
    ): Result<FavoriteResult> = safeCall(
        dispatcher = dispatcher,
        notFoundMessage = "No encontramos el artista o el coleccionista. Puede que hayan sido eliminados.",
        unexpectedMessage = "No pudimos agregar el artista a favoritos. Intenta de nuevo."
    ) {
        if (artistId in serviceAdapter.getFavoriteArtistIds(collectorId)) {
            FavoriteResult.ALREADY_FAVORITE
        } else {
            serviceAdapter.addFavoriteArtist(collectorId, artistId, type)
            FavoriteResult.ADDED
        }
    }

    private suspend fun <T> call(block: suspend () -> T): Result<T> =
        safeCall(
            dispatcher = dispatcher,
            notFoundMessage = "No encontramos este coleccionista. Puede que haya sido eliminado.",
            unexpectedMessage = "Ocurrió un error inesperado al cargar los coleccionistas.",
            call = block
        )
}
