package co.edu.uniandes.misw4203.vinilos.data.dto

/** Artista o banda que interpreta un álbum, tal como llega en el JSON del backend. */
data class PerformerDto(
    val id: Int?,
    val name: String?,
    val image: String?
)
