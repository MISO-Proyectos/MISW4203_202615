package co.edu.uniandes.misw4203.vinilos.ui.albums.detail

import co.edu.uniandes.misw4203.vinilos.data.model.ReleaseDate

/**
 * Convierte los datos del álbum en el texto que muestra el detalle (HU02).
 * No usa Context para poder probarse con pruebas locales; los nombres de los
 * meses llegan desde los recursos (R.array.months_short).
 */
object AlbumDetailFormatter {

    /** "1 ago 1984"; "1984" si solo se conoce el año; "ago 1984" si falta el día. */
    fun formatReleaseDate(date: ReleaseDate, monthNames: List<String>): String {
        val month = date.month?.let { monthNames.getOrNull(it - 1) }
            ?: return date.year.toString()
        val day = date.day ?: return "$month ${date.year}"
        return "$day $month ${date.year}"
    }

    /** Número de la canción con dos cifras: 1 → "01". */
    fun formatTrackNumber(position: Int): String =
        position.toString().padStart(2, '0')

    /**
     * Duración con minutos de dos cifras: "5:05" → "05:05". Si el texto no tiene
     * el formato minutos:segundos se muestra tal como llegó.
     */
    fun formatDuration(duration: String): String {
        val match = DURATION.matchEntire(duration.trim()) ?: return duration
        val (minutes, seconds) = match.destructured
        return "${minutes.padStart(2, '0')}:$seconds"
    }

    private val DURATION = Regex("""(\d{1,3}):(\d{2})""")
}
