package co.edu.uniandes.misw4203.vinilos.data.dto

/**
 * Copia del JSON que devuelve el backend en GET /musicians.
 * Solo trae los campos que usa el listado de artistas (HU03); Gson ignora el resto
 * (descripción, fecha de nacimiento, álbumes y premios), que se agregarán en la HU04.
 * Los campos son anulables porque Gson no respeta la nulabilidad de Kotlin.
 */
data class MusicianDto(
    val id: Int?,
    val name: String?,
    val image: String?
)
