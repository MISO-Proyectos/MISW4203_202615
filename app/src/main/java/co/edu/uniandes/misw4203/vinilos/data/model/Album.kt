package co.edu.uniandes.misw4203.vinilos.data.model

/** Álbum que usa toda la app. */
data class Album(
    val id: Int,
    val name: String,
    val cover: String,
    /** Fecha de lanzamiento, o null si el backend no envió una fecha válida. */
    val releaseDate: ReleaseDate?,
    val genre: String,
    val recordLabel: String,
    val description: String,
    val performers: List<Performer>,
    val tracks: List<Track>,
    val comments: List<Comment> = emptyList()
) {
    /** Año de lanzamiento, para las etiquetas del catálogo. */
    val releaseYear: Int?
        get() = releaseDate?.year

    /** Nombres de los intérpretes separados por coma, para mostrar en la lista. */
    val performerNames: String
        get() = performers.joinToString(", ") { it.name }
}
