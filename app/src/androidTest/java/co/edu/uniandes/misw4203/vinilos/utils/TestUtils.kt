package co.edu.uniandes.misw4203.vinilos.utils

import android.app.Activity
import android.os.Build
import android.os.SystemClock
import android.view.View
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.GrantPermissionRule
import co.edu.uniandes.misw4203.vinilos.R
import com.google.android.material.textfield.TextInputLayout
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.junit.Assert.assertEquals

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
     * Toca la vista dos veces en el mismo turno del hilo principal, así el segundo toque
     * siempre llega antes de que la pantalla cambie. Con doubleClick() no está garantizado.
     */
    fun dosToquesSeguidos(): ViewAction = object : ViewAction {
        override fun getConstraints() = isDisplayed()
        override fun getDescription() = "dos toques seguidos sobre la vista"
        override fun perform(uiController: UiController, view: View) {
            view.performClick()
            view.performClick()
            uiController.loopMainThreadUntilIdle()
        }
    }

    /** El destino actual es [actual] y justo detrás está [anterior]: la pantalla se abrió una sola vez. */
    fun verificarAbiertaUnaVez(
        scenario: ActivityScenario<out Activity>,
        actual: Int,
        anterior: Int
    ) {
        scenario.onActivity { activity ->
            val navController = activity.findNavController(R.id.nav_host_fragment)
            assertEquals(actual, navController.currentDestination?.id)
            assertEquals(anterior, navController.previousBackStackEntry?.destination?.id)
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

    /** Campo de formulario (TextInputLayout) que muestra exactamente ese mensaje de error. */
    fun conError(mensaje: String): Matcher<View> =
        object : BoundedMatcher<View, TextInputLayout>(TextInputLayout::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("campo con el error \"$mensaje\"")
            }

            override fun matchesSafely(item: TextInputLayout): Boolean =
                item.error?.toString() == mensaje
        }

    /** Campo de formulario (TextInputLayout) que no muestra ningún error. */
    fun sinError(): Matcher<View> =
        object : BoundedMatcher<View, TextInputLayout>(TextInputLayout::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("campo sin error")
            }

            override fun matchesSafely(item: TextInputLayout): Boolean = item.error.isNullOrEmpty()
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
