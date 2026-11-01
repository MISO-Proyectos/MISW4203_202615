package co.edu.uniandes.misw4203.vinilos.data.model

/**
 * Fecha de lanzamiento de un álbum. Se guarda por partes en vez de usar java.time,
 * que no existe en API 24-25. [month] (1-12) y [day] son null si el backend solo envió el año.
 */
data class ReleaseDate(
    val year: Int,
    val month: Int? = null,
    val day: Int? = null
)
