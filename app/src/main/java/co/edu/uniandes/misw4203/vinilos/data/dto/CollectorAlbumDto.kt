package co.edu.uniandes.misw4203.vinilos.data.dto

/**
 * Álbum de la colección de un coleccionista, tal como lo devuelve GET /collectors/{id}/albums.
 * En GET /collectors/{id} esta relación llega sin el álbum (solo id, precio y estado),
 * por eso la HU06 usa esta ruta para tener el nombre y la carátula.
 */
data class CollectorAlbumDto(
    val id: Int?,
    val price: Int?,
    val status: String?,
    val album: AlbumDto?
)
