package co.edu.uniandes.misw4203.vinilos.core.network

import co.edu.uniandes.misw4203.vinilos.data.dto.AlbumDto
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
}
