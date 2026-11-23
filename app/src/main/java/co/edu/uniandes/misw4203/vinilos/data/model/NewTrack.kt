package co.edu.uniandes.misw4203.vinilos.data.model

/**
 * Datos de una canción que el coleccionista quiere asociar a un álbum (HU08).
 * Todavía no tiene id: el backend lo asigna al guardarla.
 * [duration] ya viene validada y normalizada como "MM:SS".
 */
data class NewTrack(
    val name: String,
    val duration: String
)
