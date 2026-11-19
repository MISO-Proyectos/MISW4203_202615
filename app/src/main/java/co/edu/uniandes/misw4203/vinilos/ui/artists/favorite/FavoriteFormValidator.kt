package co.edu.uniandes.misw4203.vinilos.ui.artists.favorite

/** Campos del formulario "Agregar a favoritos" (HU10). Hoy solo hay uno que el usuario llena. */
enum class FavoriteFormField { COLLECTOR }

/** Problemas que puede tener un campo; el Fragment elige el texto. */
enum class FavoriteFormError { REQUIRED }

/**
 * Valida el formulario "Agregar a favoritos". No usa clases de Android para poder
 * probarse con pruebas unitarias.
 */
object FavoriteFormValidator {

    /**
     * Devuelve los campos con error; un mapa vacío significa que el formulario es válido.
     * Sin coleccionista elegido, el campo Coleccionista es REQUIRED.
     */
    fun validate(collectorId: Int?): Map<FavoriteFormField, FavoriteFormError> = buildMap {
        if (collectorId == null) put(FavoriteFormField.COLLECTOR, FavoriteFormError.REQUIRED)
    }
}
