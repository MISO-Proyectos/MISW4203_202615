package co.edu.uniandes.misw4203.vinilos.data.adapter

import co.edu.uniandes.misw4203.vinilos.core.network.BackendApi
import co.edu.uniandes.misw4203.vinilos.data.dto.AlbumDto
import co.edu.uniandes.misw4203.vinilos.data.dto.PerformerDto
import co.edu.uniandes.misw4203.vinilos.data.dto.TrackDto
import co.edu.uniandes.misw4203.vinilos.data.model.Album
import co.edu.uniandes.misw4203.vinilos.data.model.Performer
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

    companion object {

        /** Convierte un álbum del JSON en modelo. Descarta los álbumes sin id. */
        fun AlbumDto.toModel(): Album? {
            val albumId = id ?: return null
            return Album(
                id = albumId,
                name = name.orEmpty(),
                cover = cover.orEmpty(),
                releaseYear = parseReleaseYear(releaseDate),
                genre = genre.orEmpty(),
                recordLabel = recordLabel.orEmpty(),
                description = description.orEmpty(),
                performers = performers.orEmpty().mapNotNull { it.toModel() },
                tracks = tracks.orEmpty().mapNotNull { it.toModel() }
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

        /**
         * Toma el año de una fecha ISO 8601 ("1984-08-01T00:00:00.000Z").
         * Se lee el texto en vez de usar java.time porque java.time no existe en API 21-25.
         */
        fun parseReleaseYear(releaseDate: String?): Int? =
            releaseDate
                ?.takeIf { it.length >= 4 }
                ?.substring(0, 4)
                ?.toIntOrNull()
    }
}
