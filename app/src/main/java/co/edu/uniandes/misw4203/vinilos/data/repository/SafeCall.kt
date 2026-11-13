package co.edu.uniandes.misw4203.vinilos.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

private const val HTTP_NOT_FOUND = 404

/**
 * Ejecuta [call] en [dispatcher] y traduce los errores técnicos a [RepositoryException].
 * Nunca lanza excepciones, salvo la cancelación de la corrutina.
 *
 * @param notFoundMessage texto para el usuario cuando el servidor responde 404.
 * @param unexpectedMessage texto para el usuario ante cualquier otro error.
 */
suspend fun <T> safeCall(
    dispatcher: CoroutineDispatcher,
    notFoundMessage: String,
    unexpectedMessage: String,
    call: suspend () -> T
): Result<T> = withContext(dispatcher) {
    try {
        Result.success(call())
    } catch (e: CancellationException) {
        // La corrutina se canceló (por ejemplo, se cerró la pantalla): no es un error
        throw e
    } catch (e: IOException) {
        Result.failure(RepositoryException.NoConnection(e))
    } catch (e: HttpException) {
        Result.failure(
            if (e.code() == HTTP_NOT_FOUND) {
                RepositoryException.NotFound(notFoundMessage, e)
            } else {
                RepositoryException.Server(e.code(), e)
            }
        )
    } catch (e: Exception) {
        Result.failure(RepositoryException.Unexpected(unexpectedMessage, e))
    }
}
