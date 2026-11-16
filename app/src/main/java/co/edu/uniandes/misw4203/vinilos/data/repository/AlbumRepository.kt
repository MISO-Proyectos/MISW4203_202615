package co.edu.uniandes.misw4203.vinilos.data.repository

import co.edu.uniandes.misw4203.vinilos.data.adapter.AlbumServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.model.Album
import co.edu.uniandes.misw4203.vinilos.data.model.NewTrack
import co.edu.uniandes.misw4203.vinilos.data.model.Track
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Único punto al que los ViewModel piden álbumes. Nunca lanza excepciones:
 * devuelve Result.success con los datos o Result.failure con un RepositoryException.
 * Las llamadas corren en [dispatcher] (por defecto Dispatchers.IO).
 */
class AlbumRepository(
    private val serviceAdapter: AlbumServiceAdapter,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /** Último catálogo cargado con éxito. Se reemplaza entero al recargar. */
    @Volatile
    private var cachedAlbums: List<Album>? = null

    /** Borra la lista de álbumes guardada (se usa con poca memoria); la próxima consulta va a la red. */
    fun clearCache() {
        cachedAlbums = null
    }

    /**
     * HU01: catálogo de álbumes. Una lista vacía es un resultado válido (estado Empty).
     * Si ya hay un catálogo en memoria lo devuelve sin ir a la red, salvo que
     * [forceRefresh] sea true. Un fallo nunca se guarda.
     */
    suspend fun getAlbums(forceRefresh: Boolean = false): Result<List<Album>> {
        if (!forceRefresh) cachedAlbums?.let { return Result.success(it) }
        return call { serviceAdapter.getAlbums() }.onSuccess { cachedAlbums = it }
    }

    /** HU02: detalle de un álbum (no se cachea). Si no existe devuelve NotFound. */
    suspend fun getAlbum(albumId: Int): Result<Album> =
        call { serviceAdapter.getAlbum(albumId) }

    /**
     * HU08: asocia una canción a un álbum. Si el álbum no existe devuelve NotFound.
     * El detalle no se cachea, así que al volver a pedirlo ya trae la canción nueva.
     */
    suspend fun addTrack(albumId: Int, track: NewTrack): Result<Track> =
        call { serviceAdapter.addTrack(albumId, track) }

    private suspend fun <T> call(block: suspend () -> T): Result<T> =
        safeCall(
            dispatcher = dispatcher,
            notFoundMessage = "No encontramos este álbum. Puede que haya sido eliminado.",
            unexpectedMessage = "Ocurrió un error inesperado al cargar la información.",
            call = block
        )
}
