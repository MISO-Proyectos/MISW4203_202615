package co.edu.uniandes.misw4203.vinilos.data.adapter

import co.edu.uniandes.misw4203.vinilos.core.network.BackendApi
import co.edu.uniandes.misw4203.vinilos.data.dto.BandDto
import co.edu.uniandes.misw4203.vinilos.data.dto.MusicianDto
import co.edu.uniandes.misw4203.vinilos.data.model.Artist
import co.edu.uniandes.misw4203.vinilos.data.model.ArtistType
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Service Adapter de artistas: llama a la API REST y convierte el JSON (DTO)
 * en los modelos de la app. Los errores de red se propagan;
 * ArtistRepository los traduce.
 */
class ArtistServiceAdapter(
    private val api: BackendApi
) {

    /**
     * HU03: listado de artistas. El backend separa músicos y bandas en dos rutas:
     * se piden las dos a la vez y se unen ordenadas por id (el orden en que se crearon).
     * Si cualquiera de las dos falla, falla todo el listado.
     * Descarta los que lleguen sin id.
     */
    suspend fun getArtists(): List<Artist> = coroutineScope {
        val musicians = async { api.getMusicians().mapNotNull { it.toModel() } }
        val bands = async { api.getBands().mapNotNull { it.toModel() } }
        (musicians.await() + bands.await()).sortedBy { it.id }
    }

    companion object {

        /** Convierte un músico del JSON en modelo. Descarta los que no traen id. */
        fun MusicianDto.toModel(): Artist? {
            val musicianId = id ?: return null
            return Artist(
                id = musicianId,
                name = name.orEmpty(),
                image = image.orEmpty(),
                type = ArtistType.MUSICIAN
            )
        }

        /** Convierte una banda del JSON en modelo. Descarta las que no traen id. */
        fun BandDto.toModel(): Artist? {
            val bandId = id ?: return null
            return Artist(
                id = bandId,
                name = name.orEmpty(),
                image = image.orEmpty(),
                type = ArtistType.BAND
            )
        }
    }
}
