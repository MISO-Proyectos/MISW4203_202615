package co.edu.uniandes.misw4203.vinilos.data.model

/**
 * Artista del listado (HU03): puede ser un músico o una banda.
 * En el backend ambos son "performers" y comparten la misma secuencia de id.
 */
data class Artist(
    val id: Int,
    val name: String,
    val image: String,
    val type: ArtistType
)

/** Tipo de artista; la pantalla lo muestra como "Músico" o "Banda". */
enum class ArtistType {
    MUSICIAN,
    BAND
}
