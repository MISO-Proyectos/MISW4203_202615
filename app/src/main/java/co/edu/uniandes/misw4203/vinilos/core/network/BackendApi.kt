package co.edu.uniandes.misw4203.vinilos.core.network

import co.edu.uniandes.misw4203.vinilos.data.dto.AlbumDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET

/** Rutas de la API REST de Vinilos (Retrofit). */
interface BackendApi {

    /** HU01: catálogo de álbumes. */
    @GET("albums")
    suspend fun getAlbums(): List<AlbumDto>

    @GET("albums")
    suspend fun getAlbumsForHealthCheck(): Response<ResponseBody>
}
