package co.edu.uniandes.misw4203.vinilos.ui.albums.create

import co.edu.uniandes.misw4203.vinilos.data.model.AlbumGenre
import co.edu.uniandes.misw4203.vinilos.data.model.RecordLabel
import co.edu.uniandes.misw4203.vinilos.data.model.ReleaseDate

/** Campos del formulario de creación de álbum (HU07). */
enum class AlbumFormField { NAME, COVER, RELEASE_DATE, GENRE, RECORD_LABEL, DESCRIPTION }

/** Problemas que puede tener un campo del formulario; el Fragment elige el texto. */
enum class AlbumFormError { REQUIRED, INVALID_URL, FUTURE_DATE }

/**
 * Valida el formulario de creación de álbum. No usa clases de Android para poder
 * probarse con pruebas unitarias.
 */
object AlbumFormValidator {

    /** Enlace http(s) con un dominio (algo.algo) y sin espacios. */
    private val URL_REGEX = Regex("^https?://[^\\s/?#]+\\.[^\\s/?#]+(\\S*)$", RegexOption.IGNORE_CASE)

    /**
     * Devuelve los campos con error. Un mapa vacío significa que el formulario es válido.
     * [today] se usa para rechazar fechas de lanzamiento en el futuro.
     */
    fun validate(
        name: String,
        cover: String,
        releaseDate: ReleaseDate?,
        genre: AlbumGenre?,
        recordLabel: RecordLabel?,
        description: String,
        today: ReleaseDate
    ): Map<AlbumFormField, AlbumFormError> = buildMap {
        if (name.isBlank()) put(AlbumFormField.NAME, AlbumFormError.REQUIRED)

        when {
            cover.isBlank() -> put(AlbumFormField.COVER, AlbumFormError.REQUIRED)
            !isValidUrl(cover) -> put(AlbumFormField.COVER, AlbumFormError.INVALID_URL)
        }

        when {
            releaseDate == null -> put(AlbumFormField.RELEASE_DATE, AlbumFormError.REQUIRED)
            isAfter(releaseDate, today) -> put(AlbumFormField.RELEASE_DATE, AlbumFormError.FUTURE_DATE)
        }

        if (genre == null) put(AlbumFormField.GENRE, AlbumFormError.REQUIRED)
        if (recordLabel == null) put(AlbumFormField.RECORD_LABEL, AlbumFormError.REQUIRED)
        if (description.isBlank()) put(AlbumFormField.DESCRIPTION, AlbumFormError.REQUIRED)
    }

    fun isValidUrl(value: String): Boolean = URL_REGEX.matches(value.trim())

    private fun isAfter(date: ReleaseDate, other: ReleaseDate): Boolean =
        compareValuesBy(
            date, other,
            { it.year },
            { it.month ?: 1 },
            { it.day ?: 1 }
        ) > 0
}
