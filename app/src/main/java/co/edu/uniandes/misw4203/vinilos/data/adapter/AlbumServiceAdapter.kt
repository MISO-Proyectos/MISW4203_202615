package co.edu.uniandes.misw4203.vinilos.data.adapter

import co.edu.uniandes.misw4203.vinilos.core.network.BackendApi
import co.edu.uniandes.misw4203.vinilos.data.dto.AlbumDto
import co.edu.uniandes.misw4203.vinilos.data.dto.CommentDto
import co.edu.uniandes.misw4203.vinilos.data.dto.PerformerDto
import co.edu.uniandes.misw4203.vinilos.data.dto.TrackDto
import co.edu.uniandes.misw4203.vinilos.data.model.Album
import co.edu.uniandes.misw4203.vinilos.data.model.Comment
import co.edu.uniandes.misw4203.vinilos.data.model.Performer
import co.edu.uniandes.misw4203.vinilos.data.model.ReleaseDate
import co.edu.uniandes.misw4203.vinilos.data.model.Track

/**
 * Service Adapter de álbumes: llama a la API REST y convierte el JSON (DTO)
 * en los modelos de la app. Si cambia la librería de red, solo se toca esta clase.
 * Los errores de red se propagan; AlbumRepository los traduce.
 */
class AlbumServiceAdapter(
    private val api: BackendApi
) {

    suspend fun getAlbums(): List<Album> =
        api.getAlbums().mapNotNull { it.toModel() }

    /** HU02: un álbum con su detalle. Si el JSON no trae id se trata como respuesta inválida. */
    suspend fun getAlbum(albumId: Int): Album =
        api.getAlbum(albumId).toModel()
            ?: throw IllegalStateException("El backend devolvió un álbum sin id")

    companion object {

        /** Convierte un álbum del JSON en modelo. Descarta los álbumes sin id. */
        fun AlbumDto.toModel(): Album? {
            val albumId = id ?: return null
            return Album(
                id = albumId,
                name = name.orEmpty(),
                cover = cover.orEmpty(),
                releaseDate = parseReleaseDate(releaseDate),
                genre = genre.orEmpty(),
                recordLabel = recordLabel.orEmpty(),
                description = description.orEmpty(),
                performers = performers.orEmpty().mapNotNull { it.toModel() },
                tracks = tracks.orEmpty().mapNotNull { it.toModel() },
                comments = comments.orEmpty().mapNotNull { it.toModel() }
            )
        }

        fun PerformerDto.toModel(): Performer? {
            val performerId = id ?: return null
            return Performer(performerId, name.orEmpty(), image.orEmpty())
        }

        fun TrackDto.toModel(): Track? {
            val trackId = id ?: return null
            return Track(trackId, name.orEmpty(), duration.orEmpty())
        }

        fun CommentDto.toModel(): Comment? {
            val commentId = id ?: return null
            return Comment(commentId, description.orEmpty(), (rating ?: 0).coerceIn(0, 5))
        }

        /**
         * Toma el año de una fecha ISO 8601 ("1984-08-01T00:00:00.000Z").
         * Se lee el texto en vez de usar java.time porque java.time no existe en API 24-25.
         */
        fun parseReleaseYear(releaseDate: String?): Int? =
            releaseDate
                ?.takeIf { it.length >= 4 }
                ?.substring(0, 4)
                ?.toIntOrNull()

        /**
         * Toma año, mes y día de una fecha ISO 8601. Se leen los primeros 10 caracteres
         * ("1984-08-01") sin convertir de zona horaria, porque el backend guarda la fecha
         * a medianoche UTC y convertirla podría mostrar el día anterior.
         */
        fun parseReleaseDate(releaseDate: String?): ReleaseDate? {
            val year = parseReleaseYear(releaseDate) ?: return null
            val month = releaseDate?.substringOrNull(5, 7)?.toIntOrNull()?.takeIf { it in 1..12 }
            val day = releaseDate?.substringOrNull(8, 10)?.toIntOrNull()?.takeIf { it in 1..31 }
            return if (month == null) ReleaseDate(year) else ReleaseDate(year, month, day)
        }

        private fun String.substringOrNull(start: Int, end: Int): String? =
            if (length >= end) substring(start, end) else null
    }
}
