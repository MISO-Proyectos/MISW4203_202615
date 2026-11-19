package co.edu.uniandes.misw4203.vinilos.ui.albums.tracks

/** Campos del formulario "Agregar track" (HU08). */
enum class TrackFormField { NAME, DURATION }

/** Problemas que puede tener un campo; el Fragment elige el texto. */
enum class TrackFormError { REQUIRED, INVALID_DURATION }

/**
 * Valida el formulario "Agregar track". No usa clases de Android para poder
 * probarse con pruebas unitarias.
 */
object TrackFormValidator {

    /** Minutos (1 o 2 cifras) y segundos (00-59): "5:03" o "05:03". */
    private val DURATION = Regex("""^(\d{1,2}):([0-5]\d)$""")

    /**
     * Devuelve los campos con error; un mapa vacío significa que el formulario es válido.
     * Una duración vacía también se marca como formato inválido, porque el mensaje
     * ("Usa el formato MM:SS") le dice al usuario qué escribir.
     */
    fun validate(name: String, duration: String): Map<TrackFormField, TrackFormError> = buildMap {
        if (name.isBlank()) put(TrackFormField.NAME, TrackFormError.REQUIRED)
        if (normalizeDuration(duration) == null) {
            put(TrackFormField.DURATION, TrackFormError.INVALID_DURATION)
        }
    }

    /**
     * Devuelve la duración como "MM:SS" ("5:03" → "05:03"), o null si no tiene ese
     * formato o dura cero segundos.
     */
    fun normalizeDuration(duration: String): String? {
        val match = DURATION.matchEntire(duration.trim()) ?: return null
        val (minutes, seconds) = match.destructured
        if (minutes.toInt() == 0 && seconds.toInt() == 0) return null
        return "${minutes.padStart(2, '0')}:$seconds"
    }
}
