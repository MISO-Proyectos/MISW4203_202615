package co.edu.uniandes.misw4203.vinilos.data.dto

/**
 * Cuerpo que se envía en POST /albums (HU07). Los nombres de los campos coinciden
 * con los que espera el backend; [releaseDate] va en formato ISO 8601.
 */
data class AlbumCreateDto(
    val name: String,
    val cover: String,
    val releaseDate: String,
    val description: String,
    val genre: String,
    val recordLabel: String
)
