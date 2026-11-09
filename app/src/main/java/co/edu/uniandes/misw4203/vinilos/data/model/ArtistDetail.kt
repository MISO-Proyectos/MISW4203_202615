package co.edu.uniandes.misw4203.vinilos.data.model

/**
 * Detalle de un artista (HU04): incluye descripción, fecha de nacimiento o creación
 * y la lista de álbumes en los que participa.
 */
data class ArtistDetail(
    val id: Int,
    val name: String,
    val image: String,
    val type: ArtistType,
    val description: String,
    val date: ReleaseDate?,
    val albums: List<Album>
)
