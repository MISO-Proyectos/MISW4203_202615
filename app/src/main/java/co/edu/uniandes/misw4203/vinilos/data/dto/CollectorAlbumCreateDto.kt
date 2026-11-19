package co.edu.uniandes.misw4203.vinilos.data.dto

/** Cuerpo de POST /collectors/{collectorId}/albums/{albumId} (HU11). [status] es "Active" o "Inactive". */
data class CollectorAlbumCreateDto(
    val price: Int,
    val status: String
)
