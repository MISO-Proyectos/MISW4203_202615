package co.edu.uniandes.misw4203.vinilos.core.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET

interface BackendApi {

    @GET("albums")
    suspend fun getAlbumsForHealthCheck(): Response<ResponseBody>
}