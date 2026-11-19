package co.edu.uniandes.misw4203.vinilos.data.model

/** Resultado de agregar un álbum a la colección de un coleccionista (HU11). */
enum class CollectionResult {
    /** El álbum se guardó en la colección. */
    ADDED,

    /** El coleccionista ya tenía el álbum: no se envió nada al backend. */
    ALREADY_IN_COLLECTION
}
