package co.edu.uniandes.misw4203.vinilos.data.repository

import co.edu.uniandes.misw4203.vinilos.data.adapter.AlbumServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.model.Album
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

/**
 * Único punto al que los ViewModel piden álbumes. Nunca lanza excepciones:
 * devuelve Result.success con la lista o Result.failure con un AlbumRepositoryException.
 */
class AlbumRepository(
    private val serviceAdapter: AlbumServiceAdapter
) {

    /** HU01: catálogo de álbumes. Una lista vacía es un resultado válido (estado Empty). */
    suspend fun getAlbums(): Result<List<Album>> =
        try {
            Result.success(serviceAdapter.getAlbums())
        } catch (e: CancellationException) {
            // La corrutina se canceló (por ejemplo, se cerró la pantalla): no es un error
            throw e
        } catch (e: IOException) {
            Result.failure(AlbumRepositoryException.NoConnection(e))
        } catch (e: HttpException) {
            Result.failure(AlbumRepositoryException.Server(e.code(), e))
        } catch (e: Exception) {
            Result.failure(AlbumRepositoryException.Unexpected(e))
        }
}
