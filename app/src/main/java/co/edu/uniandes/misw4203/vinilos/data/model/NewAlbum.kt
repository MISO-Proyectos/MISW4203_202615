package co.edu.uniandes.misw4203.vinilos.data.model

/**
 * Datos de un álbum que el coleccionista quiere crear (HU07). Todavía no tiene id:
 * el backend lo asigna al guardarlo.
 */
data class NewAlbum(
    val name: String,
    val cover: String,
    /** Fecha completa (año, mes y día). */
    val releaseDate: ReleaseDate,
    val genre: AlbumGenre,
    val recordLabel: RecordLabel,
    val description: String
)

/** Géneros que acepta el backend; [apiValue] es el texto exacto que se envía. */
enum class AlbumGenre(val apiValue: String) {
    CLASSICAL("Classical"),
    SALSA("Salsa"),
    ROCK("Rock"),
    FOLK("Folk")
}

/** Sellos discográficos que acepta el backend; [apiValue] es el texto exacto que se envía. */
enum class RecordLabel(val apiValue: String) {
    SONY("Sony Music"),
    EMI("EMI"),
    DISCOS_FUENTES("Discos Fuentes"),
    ELEKTRA("Elektra"),
    FANIA("Fania Records")
}
