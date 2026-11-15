package co.edu.uniandes.misw4203.vinilos

import android.app.Application
import android.content.ComponentCallbacks2
import android.os.StrictMode
import co.edu.uniandes.misw4203.vinilos.di.AppContainer
import com.bumptech.glide.Glide

class VinilosApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) enableStrictMode()
    }

    /** Solo en debug: avisa en Logcat (etiqueta StrictMode) de trabajo en el hilo principal y de fugas. */
    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
    }

    /** Se guarda aparte para saber si ya se creó antes de limpiar. */
    private val appContainerLazy: Lazy<AppContainer> = lazy {
        AppContainer(BuildConfig.BASE_URL)
    }

    val appContainer: AppContainer by appContainerLazy

    /** El sistema pide memoria: libera las imágenes y, si la app quedó en segundo plano, las listas. */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Glide.get(this).trimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) clearRepositoryCaches()
    }

    /** Poca memoria: libera todo lo que se pueda volver a bajar. */
    @Suppress("DEPRECATION")
    override fun onLowMemory() {
        super.onLowMemory()
        Glide.get(this).clearMemory()
        clearRepositoryCaches()
    }

    /** Si el contenedor no se ha creado no hay nada que limpiar, así que no lo forzamos. */
    private fun clearRepositoryCaches() {
        if (!appContainerLazy.isInitialized()) return
        appContainer.albumRepository.clearCache()
        appContainer.artistRepository.clearCache()
        appContainer.collectorRepository.clearCache()
    }
}