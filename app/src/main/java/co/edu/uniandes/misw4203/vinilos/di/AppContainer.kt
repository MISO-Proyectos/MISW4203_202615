package co.edu.uniandes.misw4203.vinilos.di

import co.edu.uniandes.misw4203.vinilos.BuildConfig
import co.edu.uniandes.misw4203.vinilos.core.network.BackendApi
import co.edu.uniandes.misw4203.vinilos.data.ConnectionRepository
import co.edu.uniandes.misw4203.vinilos.data.adapter.AlbumServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.repository.AlbumRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Arma a mano las piezas de la app (Retrofit, adapters y repositorios) y las comparte.
 * Se crea una sola vez en VinilosApplication.
 */
class AppContainer(baseUrl: String) {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BASIC
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val backendApi = retrofit.create(BackendApi::class.java)

    private val albumServiceAdapter = AlbumServiceAdapter(backendApi)

    /** HU01 y HU02: álbumes. */
    val albumRepository = AlbumRepository(albumServiceAdapter)

    val connectionRepository = ConnectionRepository(backendApi)
}
