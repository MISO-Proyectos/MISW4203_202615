package co.edu.uniandes.misw4203.vinilos.data.repository

import co.edu.uniandes.misw4203.vinilos.data.adapter.ArtistServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.model.Artist
import co.edu.uniandes.misw4203.vinilos.data.model.ArtistDetail
import co.edu.uniandes.misw4203.vinilos.data.model.ArtistType
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

/**
 * Único punto al que los ViewModel piden artistas. Nunca lanza excepciones:
 * devuelve Result.success con los datos o Result.failure con un ArtistRepositoryException.
 */
class ArtistRepository(
    private val serviceAdapter: ArtistServiceAdapter
) {

    /** HU03: listado de artistas (músicos y bandas). Una lista vacía es un resultado válido (estado Empty). */
    suspend fun getArtists(): Result<List<Artist>> =
        safeCall { serviceAdapter.getArtists() }

    /** HU04: detalle de un artista (músico o banda). */
    suspend fun getArtistDetail(id: Int, type: ArtistType): Result<ArtistDetail> =
        safeCall { serviceAdapter.getArtistDetail(id, type) }

    /** Ejecuta la llamada y traduce los errores técnicos a ArtistRepositoryException. */
    private suspend fun <T> safeCall(call: suspend () -> T): Result<T> =
        try {
            Result.success(call())
        } catch (e: CancellationException) {
            // La corrutina se canceló (por ejemplo, se cerró la pantalla): no es un error
            throw e
        } catch (e: IOException) {
            Result.failure(ArtistRepositoryException.NoConnection(e))
        } catch (e: HttpException) {
            Result.failure(ArtistRepositoryException.Server(e.code(), e))
        } catch (e: Exception) {
            Result.failure(ArtistRepositoryException.Unexpected(e))
        }
}
