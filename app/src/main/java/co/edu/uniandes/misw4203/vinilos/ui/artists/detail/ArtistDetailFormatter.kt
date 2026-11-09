package co.edu.uniandes.misw4203.vinilos.ui.artists.detail

import co.edu.uniandes.misw4203.vinilos.data.model.ReleaseDate

/** Formateador para las fechas en el detalle del artista. */
object ArtistDetailFormatter {

    fun formatDate(date: ReleaseDate, monthsShort: List<String>): String {
        val month = date.month?.let { monthsShort.getOrNull(it - 1) }
        return if (month != null && date.day != null) {
            "${date.day} $month ${date.year}"
        } else if (month != null) {
            "$month ${date.year}"
        } else {
            "${date.year}"
        }
    }
}
