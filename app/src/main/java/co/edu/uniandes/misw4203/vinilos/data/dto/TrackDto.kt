package co.edu.uniandes.misw4203.vinilos.data.dto

/** Canción de un álbum, tal como llega en el JSON del backend. */
data class TrackDto(
    val id: Int?,
    val name: String?,
    val duration: String?
)
