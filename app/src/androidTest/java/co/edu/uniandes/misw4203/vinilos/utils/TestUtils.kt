package co.edu.uniandes.misw4203.vinilos.utils

import android.os.Build
import android.os.SystemClock
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.GrantPermissionRule
import co.edu.uniandes.misw4203.vinilos.R

object TestUtils {

    /**
     * Android 17 (API 37) pide permiso para conectarse a la red local (10.0.2.2, donde corre
     * el backend). Se concede antes de abrir la app para que el diálogo no tape la pantalla.
     * En versiones anteriores ese permiso no existe y la regla no hace nada.
     */
    fun grantLocalNetworkAccess(): GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= 37) {
            GrantPermissionRule.grant("android.permission.ACCESS_LOCAL_NETWORK")
        } else {
            GrantPermissionRule.grant()
        }

    /**
     * Hace clic directo sobre la vista. La flecha de volver de la barra superior queda 8 dp
     * dentro del relleno de su contenedor, y el clic normal de Espresso exige ver al menos
     * el 90 % de la vista; esta acción solo pide que se esté mostrando.
     */
    fun clicDirecto(): ViewAction = object : ViewAction {
        override fun getConstraints() = isDisplayed()
        override fun getDescription() = "clic directo sobre la vista"
        override fun perform(uiController: UiController, view: View) {
            view.performClick()
        }
    }

    /**
     * Desde el catálogo, toca la tarjeta del álbum con ese nombre para abrir su detalle.
     * Espera a que la lista esté cargada y se desplaza hasta la tarjeta si hace falta.
     */
    fun abrirDetalleDeAlbum(nombre: String) {
        waitFor { onView(withId(R.id.albumList)).check(matches(hasDescendant(withText(nombre)))) }
        onView(withId(R.id.albumList)).perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(nombre)),
                click()
            )
        )
    }

    /**
     * Repite la verificación hasta que pase o se agote el tiempo.
     * La respuesta de la red llega después de que la pantalla se abre,
     * así que no se puede verificar de inmediato.
     */
    fun waitFor(
        timeoutMs: Long = 10_000,
        intervalMs: Long = 250,
        check: () -> Unit
    ) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        var lastError: Throwable? = null

        while (SystemClock.elapsedRealtime() < deadline) {
            try {
                check()
                return
            } catch (e: Throwable) {
                lastError = e
                Thread.sleep(intervalMs)
            }
        }
        throw lastError ?: AssertionError("Tiempo de espera agotado")
    }
}
