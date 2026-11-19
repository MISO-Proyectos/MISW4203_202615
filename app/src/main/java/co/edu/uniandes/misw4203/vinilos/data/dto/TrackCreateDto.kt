package co.edu.uniandes.misw4203.vinilos.data.dto

/** Cuerpo que se envía en POST /albums/{id}/tracks (HU08). [duration] va como "05:03". */
data class TrackCreateDto(
    val name: String,
    val duration: String
)
