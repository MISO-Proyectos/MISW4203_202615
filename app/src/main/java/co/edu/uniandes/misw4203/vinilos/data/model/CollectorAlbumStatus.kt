package co.edu.uniandes.misw4203.vinilos.data.model

/** Estado de un álbum en la colección de un coleccionista; [apiValue] es el texto que espera el backend. */
enum class CollectorAlbumStatus(val apiValue: String) {
    ACTIVE("Active"),
    INACTIVE("Inactive")
}
