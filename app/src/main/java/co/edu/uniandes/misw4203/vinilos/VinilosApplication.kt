package co.edu.uniandes.misw4203.vinilos

import android.app.Application
import co.edu.uniandes.misw4203.vinilos.BuildConfig
import co.edu.uniandes.misw4203.vinilos.di.AppContainer

class VinilosApplication : Application() {
    val appContainer: AppContainer by lazy {
        AppContainer(BuildConfig.BASE_URL)
    }
}