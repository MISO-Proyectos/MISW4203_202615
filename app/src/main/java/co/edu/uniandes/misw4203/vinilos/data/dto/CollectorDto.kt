package co.edu.uniandes.misw4203.vinilos.data.dto

/**
 * Copia del JSON que devuelve el backend en GET /collectors.
 * Solo trae los campos que usa el listado (HU05); Gson ignora el resto
 * (comentarios, artistas favoritos y álbumes), que se agregarán en la HU06.
 * Los campos son anulables porque Gson no respeta la nulabilidad de Kotlin:
 * si el backend omite un campo, el adapter pone un valor por defecto.
 */
data class CollectorDto(
    val id: Int?,
    val name: String?,
    val telephone: String?,
    val email: String?
)
