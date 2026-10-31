package co.edu.uniandes.misw4203.vinilos.data

import co.edu.uniandes.misw4203.vinilos.core.network.BackendApi
import co.edu.uniandes.misw4203.vinilos.data.dto.AlbumDto
import co.edu.uniandes.misw4203.vinilos.data.dto.PerformerDto
import co.edu.uniandes.misw4203.vinilos.data.dto.TrackDto
import okhttp3.ResponseBody
import retrofit2.Response

/** API falsa para las pruebas: devuelve la lista dada o lanza el error dado. */
class FakeBackendApi(
    private val albums: List<AlbumDto> = emptyList(),
    private val error: Throwable? = null
) : BackendApi {

    override suspend fun getAlbums(): List<AlbumDto> {
        error?.let { throw it }
        return albums
    }

    override suspend fun getAlbumsForHealthCheck(): Response<ResponseBody> =
        throw UnsupportedOperationException("No se usa en estas pruebas")

    companion object {
        fun albumDto(
            id: Int? = 100,
            name: String? = "Buscando América",
            releaseDate: String? = "1984-08-01T00:00:00.000Z",
            performers: List<PerformerDto>? = listOf(PerformerDto(1, "Rubén Blades", "rb.jpg")),
            tracks: List<TrackDto>? = listOf(TrackDto(10, "Decisiones", "5:05"))
        ) = AlbumDto(
            id = id,
            name = name,
            cover = "https://example.com/cover.jpg",
            releaseDate = releaseDate,
            description = "Álbum de salsa",
            genre = "Salsa",
            recordLabel = "Elektra",
            tracks = tracks,
            performers = performers
        )
    }
}
