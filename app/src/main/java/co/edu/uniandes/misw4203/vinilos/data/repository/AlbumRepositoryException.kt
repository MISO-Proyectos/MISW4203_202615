package co.edu.uniandes.misw4203.vinilos.data.repository

/**
 * Errores que AlbumRepository entrega a los ViewModel, con un mensaje entendible
 * para el usuario en vez del error técnico (IOException, HttpException, etc.).
 */
sealed class AlbumRepositoryException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    /** No hay internet o no se pudo llegar al servidor. */
    class NoConnection(cause: Throwable? = null) : AlbumRepositoryException(
        "No hay conexión con el servidor. Revisa tu internet e inténtalo de nuevo.",
        cause
    )

    /** El servidor respondió con un error HTTP. */
    class Server(val code: Int, cause: Throwable? = null) : AlbumRepositoryException(
        "El servidor no pudo responder (error $code). Inténtalo más tarde.",
        cause
    )

    /** HU02: el álbum pedido no existe (HTTP 404). */
    class NotFound(cause: Throwable? = null) : AlbumRepositoryException(
        "No encontramos este álbum. Puede que haya sido eliminado.",
        cause
    )

    /** Cualquier otro problema, por ejemplo un JSON con un formato inesperado. */
    class Unexpected(cause: Throwable? = null) : AlbumRepositoryException(
        "Ocurrió un error inesperado al cargar la información.",
        cause
    )
}
