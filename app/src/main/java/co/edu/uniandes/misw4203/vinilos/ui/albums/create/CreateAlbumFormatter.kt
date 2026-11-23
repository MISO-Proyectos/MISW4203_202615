package co.edu.uniandes.misw4203.vinilos.ui.albums.create

import co.edu.uniandes.misw4203.vinilos.data.model.ReleaseDate
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Conversiones de fecha del formulario "Agregar álbum" (HU07). El calendario de Material
 * trabaja con milisegundos a medianoche UTC; aquí se pasan a [ReleaseDate] y al texto
 * DD/MM/AAAA del campo, sin java.time (no existe en API 24-25).
 */
object CreateAlbumFormatter {

    private val UTC: TimeZone = TimeZone.getTimeZone("UTC")

    /** "01/08/1984". Si falta el mes o el día se usa 1. */
    fun formatDate(date: ReleaseDate): String =
        String.format(Locale.US, "%02d/%02d/%04d", date.day ?: 1, date.month ?: 1, date.year)

    /** Milisegundos (medianoche UTC) que devuelve el calendario → fecha. */
    fun fromUtcMillis(millis: Long): ReleaseDate {
        val calendar = Calendar.getInstance(UTC).apply { timeInMillis = millis }
        return ReleaseDate(
            year = calendar.get(Calendar.YEAR),
            month = calendar.get(Calendar.MONTH) + 1,
            day = calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    /** Fecha → milisegundos a medianoche UTC, para marcarla en el calendario. */
    fun toUtcMillis(date: ReleaseDate): Long =
        Calendar.getInstance(UTC).apply {
            clear()
            set(date.year, (date.month ?: 1) - 1, date.day ?: 1)
        }.timeInMillis
}
