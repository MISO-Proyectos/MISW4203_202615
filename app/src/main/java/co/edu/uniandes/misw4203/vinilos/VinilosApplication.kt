package co.edu.uniandes.misw4203.vinilos

import android.app.Application
import android.os.StrictMode
import co.edu.uniandes.misw4203.vinilos.di.AppContainer

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

    val appContainer: AppContainer by lazy {
        AppContainer(BuildConfig.BASE_URL)
    }
}