package co.edu.uniandes.misw4203.vinilos.data

import co.edu.uniandes.misw4203.vinilos.core.network.BackendApi
import co.edu.uniandes.misw4203.vinilos.data.dto.AlbumDto
import co.edu.uniandes.misw4203.vinilos.data.dto.BandDto
import co.edu.uniandes.misw4203.vinilos.data.dto.CollectorAlbumDto
import co.edu.uniandes.misw4203.vinilos.data.dto.CollectorDto
import co.edu.uniandes.misw4203.vinilos.data.dto.CommentDto
import co.edu.uniandes.misw4203.vinilos.data.dto.MusicianDto
import co.edu.uniandes.misw4203.vinilos.data.dto.PerformerDto
import co.edu.uniandes.misw4203.vinilos.data.dto.TrackDto
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

/**
 * API falsa para las pruebas: devuelve las listas dadas o lanza el error dado.
 * getAlbum busca por id en la misma lista y responde HTTP 404 si no lo encuentra.
 */
class FakeBackendApi(
    /** Se puede cambiar para simular que el backend tiene álbumes nuevos. */
    var albums: List<AlbumDto> = emptyList(),
    /** Si no es null, todas las llamadas lanzan este error. Se puede cambiar para simular que vuelve la red. */
    var error: Throwable? = null,
    /** Se puede cambiar para simular que el backend tiene coleccionistas nuevos. */
    var collectors: List<CollectorDto> = emptyList(),
    /** Álbumes de la colección que devuelve GET /collectors/{id}/albums (iguales para cualquier coleccionista). */
    private val collectorAlbums: List<CollectorAlbumDto> = emptyList(),
    /** Se puede cambiar para simular que el backend tiene artistas nuevos. */
    var musicians: List<MusicianDto> = emptyList(),
    var bands: List<BandDto> = emptyList(),
    /** Si no es null, solo GET /bands lanza este error (para probar que falle una de las dos rutas). */
    var bandsError: Throwable? = null
) : BackendApi {

    /** Cuántas veces se pidió un listado (álbumes, coleccionistas o músicos) a la "red". */
    var listRequests = 0
        private set

    override suspend fun getAlbums(): List<AlbumDto> {
        listRequests++
        error?.let { throw it }
        return albums
    }

    override suspend fun getAlbum(albumId: Int): AlbumDto {
        error?.let { throw it }
        return albums.firstOrNull { it.id == albumId }
            ?: throw HttpException(Response.error<Any>(404, "".toResponseBody(null)))
    }

    override suspend fun getCollectors(): List<CollectorDto> {
        listRequests++
        error?.let { throw it }
        return collectors
    }

    override suspend fun getCollector(id: Int): CollectorDto {
        error?.let { throw it }
        return collectors.firstOrNull { it.id == id }
            ?: throw HttpException(Response.error<Any>(404, "".toResponseBody(null)))
    }

    override suspend fun getCollectorAlbums(id: Int): List<CollectorAlbumDto> {
        error?.let { throw it }
        if (collectors.none { it.id == id }) {
            throw HttpException(Response.error<Any>(404, "".toResponseBody(null)))
        }
        return collectorAlbums
    }

    override suspend fun getMusicians(): List<MusicianDto> {
        listRequests++
        error?.let { throw it }
        return musicians
    }

    override suspend fun getBands(): List<BandDto> {
        error?.let { throw it }
        bandsError?.let { throw it }
        return bands
    }

    override suspend fun getMusician(id: Int): MusicianDto {
        error?.let { throw it }
        return musicians.firstOrNull { it.id == id }
            ?: throw HttpException(Response.error<Any>(404, "".toResponseBody(null)))
    }

    override suspend fun getBand(id: Int): BandDto {
        error?.let { throw it }
        bandsError?.let { throw it }
        return bands.firstOrNull { it.id == id }
            ?: throw HttpException(Response.error<Any>(404, "".toResponseBody(null)))
    }

    companion object {
        fun musicianDto(
            id: Int? = 100,
            name: String? = "Rubén Blades Bellido de Luna",
            image: String? = "https://example.com/ruben.jpg",
            description: String? = "Cantante y compositor panameño.",
            birthDate: String? = "1948-07-16T00:00:00.000Z",
            albums: List<AlbumDto>? = listOf(albumDto())
        ) = MusicianDto(id, name, image, description, birthDate, albums)

        fun bandDto(
            id: Int? = 101,
            name: String? = "Queen",
            image: String? = "https://example.com/queen.jpg",
            description: String? = "Banda británica de rock.",
            creationDate: String? = "1970-01-01T00:00:00.000Z",
            albums: List<AlbumDto>? = listOf(albumDto(102, "A Night at the Opera"))
        ) = BandDto(id, name, image, description, creationDate, albums)

        fun collectorDto(
            id: Int? = 100,
            name: String? = "Manolo Bellon",
            telephone: String? = "3502457896",
            email: String? = "manollo@caracol.com.co",
            comments: List<CommentDto>? = null,
            favoritePerformers: List<PerformerDto>? = null
        ) = CollectorDto(id, name, telephone, email, comments, favoritePerformers)

        fun collectorAlbumDto(
            id: Int? = 100,
            price: Int? = 35,
            status: String? = "Active",
            album: AlbumDto? = albumDto()
        ) = CollectorAlbumDto(id, price, status, album)

        fun albumDto(
            id: Int? = 100,
            name: String? = "Buscando América",
            releaseDate: String? = "1984-08-01T00:00:00.000Z",
            performers: List<PerformerDto>? = listOf(PerformerDto(1, "Rubén Blades", "rb.jpg")),
            tracks: List<TrackDto>? = listOf(TrackDto(10, "Decisiones", "5:05")),
            comments: List<CommentDto>? = listOf(CommentDto(1, "Un clásico", 5))
        ) = AlbumDto(
            id = id,
            name = name,
            cover = "https://example.com/cover.jpg",
            releaseDate = releaseDate,
            description = "Álbum de salsa",
            genre = "Salsa",
            recordLabel = "Elektra",
            tracks = tracks,
            performers = performers,
            comments = comments
        )
    }
}
