package co.edu.uniandes.misw4203.vinilos.data.repository

/**
 * Errores que los repositorios entregan a los ViewModel, con un mensaje entendible
 * para el usuario en vez del error técnico (IOException, HttpException, etc.).
 * Los textos de "no encontrado" e "inesperado" dependen de la entidad: cada repositorio
 * se los pasa a [safeCall].
 */
sealed class RepositoryException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    /** No hay internet o no se pudo llegar al servidor. */
    class NoConnection(cause: Throwable? = null) : RepositoryException(
        "No hay conexión con el servidor. Revisa tu internet e inténtalo de nuevo.",
        cause
    )

    /** El servidor respondió con un error HTTP distinto de 404. */
    class Server(val code: Int, cause: Throwable? = null) : RepositoryException(
        "El servidor no pudo responder (error $code). Inténtalo más tarde.",
        cause
    )

    /** El recurso pedido no existe (HTTP 404). */
    class NotFound(message: String, cause: Throwable? = null) : RepositoryException(message, cause)

    /** Cualquier otro problema, por ejemplo un JSON con un formato inesperado. */
    class Unexpected(message: String, cause: Throwable? = null) : RepositoryException(message, cause)
}
