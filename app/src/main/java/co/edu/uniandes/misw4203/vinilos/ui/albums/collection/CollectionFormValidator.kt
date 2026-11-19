package co.edu.uniandes.misw4203.vinilos.ui.albums.collection

import co.edu.uniandes.misw4203.vinilos.data.model.CollectorAlbumStatus

/** Campos del formulario "Agregar a colección" (HU11). */
enum class CollectionFormField { COLLECTOR, PRICE, STATUS }

/** Problemas que puede tener un campo; el Fragment elige el texto. */
enum class CollectionFormError { REQUIRED, INVALID_PRICE }

/**
 * Valida el formulario "Agregar a colección". No usa clases de Android para poder
 * probarse con pruebas unitarias.
 */
object CollectionFormValidator {

    /** Hasta 9 dígitos para que el precio quepa en un Int. */
    private val PRICE = Regex("""^\d{1,9}$""")

    /**
     * Devuelve los campos con error; un mapa vacío significa que el formulario es válido.
     * Un precio vacío es REQUIRED; uno con letras, puntos, ceros o demasiado largo, INVALID_PRICE.
     */
    fun validate(
        collectorId: Int?,
        price: String,
        status: CollectorAlbumStatus?
    ): Map<CollectionFormField, CollectionFormError> = buildMap {
        if (collectorId == null) put(CollectionFormField.COLLECTOR, CollectionFormError.REQUIRED)
        when {
            price.isBlank() -> put(CollectionFormField.PRICE, CollectionFormError.REQUIRED)
            parsePrice(price) == null -> put(CollectionFormField.PRICE, CollectionFormError.INVALID_PRICE)
        }
        if (status == null) put(CollectionFormField.STATUS, CollectionFormError.REQUIRED)
    }

    /**
     * Devuelve el precio como entero, o null si no son solo dígitos (1 a 9 tras quitar
     * los espacios de los extremos) o si es cero.
     */
    fun parsePrice(price: String): Int? {
        val text = price.trim()
        if (!PRICE.matches(text)) return null
        return text.toInt().takeIf { it > 0 }
    }
}
