package co.edu.uniandes.misw4203.vinilos.data.model

/** Resultado de agregar un artista a los favoritos de un coleccionista (HU10). */
enum class FavoriteResult {
    /** El artista se guardó en los favoritos. */
    ADDED,

    /** El coleccionista ya tenía al artista: no se envió nada al backend. */
    ALREADY_FAVORITE
}
