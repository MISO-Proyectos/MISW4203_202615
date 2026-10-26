package co.edu.uniandes.misw4203.vinilos.data.model

/** Álbum que usa toda la app. */
data class Album(
    val id: Int,
    val name: String,
    val cover: String,
    /** Año de lanzamiento, o null si el backend no envió una fecha válida. */
    val releaseYear: Int?,
    val genre: String,
    val recordLabel: String,
    val description: String,
    val performers: List<Performer>,
    val tracks: List<Track>
) {
    /** Nombres de los intérpretes separados por coma, para mostrar en la lista. */
    val performerNames: String
        get() = performers.joinToString(", ") { it.name }
}
