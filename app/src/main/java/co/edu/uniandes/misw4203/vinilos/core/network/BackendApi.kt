package co.edu.uniandes.misw4203.vinilos.core.network

import co.edu.uniandes.misw4203.vinilos.data.dto.AlbumDto
import co.edu.uniandes.misw4203.vinilos.data.dto.BandDto
import co.edu.uniandes.misw4203.vinilos.data.dto.CollectorAlbumDto
import co.edu.uniandes.misw4203.vinilos.data.dto.CollectorDto
import co.edu.uniandes.misw4203.vinilos.data.dto.MusicianDto
import retrofit2.http.GET
import retrofit2.http.Path

/** Rutas de la API REST de Vinilos (Retrofit). */
interface BackendApi {

    /** HU01: catálogo de álbumes. */
    @GET("albums")
    suspend fun getAlbums(): List<AlbumDto>

    /** HU02: detalle de un álbum, con sus tracks, intérpretes y comentarios. */
    @GET("albums/{id}")
    suspend fun getAlbum(@Path("id") albumId: Int): AlbumDto

    /** HU05: listado de coleccionistas. */
    @GET("collectors")
    suspend fun getCollectors(): List<CollectorDto>

    /** HU06: detalle de un coleccionista, con comentarios, artistas favoritos y su colección (sin nombre de álbum). */
    @GET("collectors/{id}")
    suspend fun getCollector(@Path("id") id: Int): CollectorDto

    /** HU06: álbumes de la colección de un coleccionista, con precio, estado y datos del álbum. */
    @GET("collectors/{id}/albums")
    suspend fun getCollectorAlbums(@Path("id") id: Int): List<CollectorAlbumDto>

    /** HU03: listado de artistas (músicos). */
    @GET("musicians")
    suspend fun getMusicians(): List<MusicianDto>

    /** HU03: listado de artistas (bandas). */
    @GET("bands")
    suspend fun getBands(): List<BandDto>

    /** HU04: detalle de un músico. */
    @GET("musicians/{id}")
    suspend fun getMusician(@Path("id") id: Int): MusicianDto

    /** HU04: detalle de una banda. */
    @GET("bands/{id}")
    suspend fun getBand(@Path("id") id: Int): BandDto
}
