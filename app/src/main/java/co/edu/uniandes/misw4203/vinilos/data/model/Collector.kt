package co.edu.uniandes.misw4203.vinilos.data.model

/** Coleccionista que usa toda la app (HU05 y HU06). */
data class Collector(
    val id: Int,
    val name: String,
    val telephone: String,
    val email: String
) {
    /**
     * Iniciales para el avatar de la lista: "Manolo Bellon" da "MB".
     * Usa las dos primeras palabras del nombre y devuelve "?" si no hay nombre.
     */
    val initials: String
        get() = name.trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "?" }
}
