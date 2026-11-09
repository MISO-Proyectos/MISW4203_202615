package co.edu.uniandes.misw4203.vinilos.data.model

/** Álbum que tiene un coleccionista, con el precio y el estado ("Active" o "Inactive") de la relación. */
data class CollectorAlbum(
    val id: Int,
    val price: Int,
    val status: String,
    val album: Album
)
