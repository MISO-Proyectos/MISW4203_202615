package co.edu.uniandes.misw4203.vinilos.data.adapter

import co.edu.uniandes.misw4203.vinilos.core.network.BackendApi
import co.edu.uniandes.misw4203.vinilos.data.adapter.AlbumServiceAdapter.Companion.parseReleaseDate
import co.edu.uniandes.misw4203.vinilos.data.adapter.AlbumServiceAdapter.Companion.toModel
import co.edu.uniandes.misw4203.vinilos.data.dto.BandDto
import co.edu.uniandes.misw4203.vinilos.data.dto.MusicianDto
import co.edu.uniandes.misw4203.vinilos.data.model.Artist
import co.edu.uniandes.misw4203.vinilos.data.model.ArtistDetail
import co.edu.uniandes.misw4203.vinilos.data.model.ArtistType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Service Adapter de artistas: llama a la API REST y convierte el JSON (DTO)
 * en los modelos de la app. Los errores de red se propagan;
 * ArtistRepository los traduce. La conversión DTO → modelo y el ordenamiento corren en
 * [dispatcher] (por defecto Dispatchers.Default).
 */
class ArtistServiceAdapter(
    private val api: BackendApi,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {

    /**
     * HU03: listado de artistas. El backend separa músicos y bandas en dos rutas:
     * se piden las dos a la vez y se unen ordenadas por id (el orden en que se crearon).
     * Si cualquiera de las dos falla, falla todo el listado.
     * Descarta los que lleguen sin id.
     */
    suspend fun getArtists(): List<Artist> = coroutineScope {
        val musicians = async { api.getMusicians() }
        val bands = async { api.getBands() }
        val musicianDtos = musicians.await()
        val bandDtos = bands.await()
        withContext(dispatcher) {
            (musicianDtos.mapNotNull { it.toModel() } + bandDtos.mapNotNull { it.toModel() })
                .sortedBy { it.id }
        }
    }

    /**
     * HU04: detalle de un artista (músico o banda).
     */
    suspend fun getArtistDetail(id: Int, type: ArtistType): ArtistDetail =
        when (type) {
            ArtistType.MUSICIAN -> {
                val dto = api.getMusician(id)
                withContext(dispatcher) {
                    dto.toDetailModel()
                        ?: throw IllegalStateException("El backend devolvió un músico sin id")
                }
            }
            ArtistType.BAND -> {
                val dto = api.getBand(id)
                withContext(dispatcher) {
                    dto.toDetailModel()
                        ?: throw IllegalStateException("El backend devolvió una banda sin id")
                }
            }
        }

    companion object {

        /** Convierte un músico del JSON en modelo del listado. Descarta los que no traen id. */
        fun MusicianDto.toModel(): Artist? {
            val musicianId = id ?: return null
            return Artist(
                id = musicianId,
                name = name.orEmpty(),
                image = image.orEmpty(),
                type = ArtistType.MUSICIAN
            )
        }

        /** Convierte una banda del JSON en modelo del listado. Descarta las que no traen id. */
        fun BandDto.toModel(): Artist? {
            val bandId = id ?: return null
            return Artist(
                id = bandId,
                name = name.orEmpty(),
                image = image.orEmpty(),
                type = ArtistType.BAND
            )
        }

        /** Convierte un músico del JSON en el modelo detallado. */
        fun MusicianDto.toDetailModel(): ArtistDetail? {
            val musicianId = id ?: return null
            return ArtistDetail(
                id = musicianId,
                name = name.orEmpty(),
                image = image.orEmpty(),
                type = ArtistType.MUSICIAN,
                description = description.orEmpty(),
                date = parseReleaseDate(birthDate),
                albums = albums.orEmpty().mapNotNull { it.toModel() }
            )
        }

        /** Convierte una banda del JSON en el modelo detallado. */
        fun BandDto.toDetailModel(): ArtistDetail? {
            val bandId = id ?: return null
            return ArtistDetail(
                id = bandId,
                name = name.orEmpty(),
                image = image.orEmpty(),
                type = ArtistType.BAND,
                description = description.orEmpty(),
                date = parseReleaseDate(creationDate),
                albums = albums.orEmpty().mapNotNull { it.toModel() }
            )
        }
    }
}
