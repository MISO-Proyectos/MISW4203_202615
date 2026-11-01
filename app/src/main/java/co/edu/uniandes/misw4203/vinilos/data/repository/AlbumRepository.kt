package co.edu.uniandes.misw4203.vinilos.data.repository

import co.edu.uniandes.misw4203.vinilos.data.adapter.AlbumServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.model.Album
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

/**
 * Único punto al que los ViewModel piden álbumes. Nunca lanza excepciones:
 * devuelve Result.success con los datos o Result.failure con un AlbumRepositoryException.
 */
class AlbumRepository(
    private val serviceAdapter: AlbumServiceAdapter
) {

    /** HU01: catálogo de álbumes. Una lista vacía es un resultado válido (estado Empty). */
    suspend fun getAlbums(): Result<List<Album>> =
        safeCall { serviceAdapter.getAlbums() }

    /** HU02: detalle de un álbum. Si el álbum no existe devuelve NotFound. */
    suspend fun getAlbum(albumId: Int): Result<Album> =
        safeCall { serviceAdapter.getAlbum(albumId) }

    /** Ejecuta la llamada y traduce los errores técnicos a AlbumRepositoryException. */
    private suspend fun <T> safeCall(call: suspend () -> T): Result<T> =
        try {
            Result.success(call())
        } catch (e: CancellationException) {
            // La corrutina se canceló (por ejemplo, se cerró la pantalla): no es un error
            throw e
        } catch (e: IOException) {
            Result.failure(AlbumRepositoryException.NoConnection(e))
        } catch (e: HttpException) {
            val error = if (e.code() == HTTP_NOT_FOUND) {
                AlbumRepositoryException.NotFound(e)
            } else {
                AlbumRepositoryException.Server(e.code(), e)
            }
            Result.failure(error)
        } catch (e: Exception) {
            Result.failure(AlbumRepositoryException.Unexpected(e))
        }

    private companion object {
        const val HTTP_NOT_FOUND = 404
    }
}
