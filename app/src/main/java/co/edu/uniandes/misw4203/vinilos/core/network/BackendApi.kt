package co.edu.uniandes.misw4203.vinilos.core.network

import co.edu.uniandes.misw4203.vinilos.data.dto.AlbumDto
import co.edu.uniandes.misw4203.vinilos.data.dto.BandDto
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

    /** HU03: listado de artistas (músicos). */
    @GET("musicians")
    suspend fun getMusicians(): List<MusicianDto>

    /** HU03: listado de artistas (bandas). */
    @GET("bands")
    suspend fun getBands(): List<BandDto>
}
