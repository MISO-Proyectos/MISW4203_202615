package co.edu.uniandes.misw4203.vinilos.role

import android.view.View
import androidx.navigation.findNavController
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import co.edu.uniandes.misw4203.vinilos.MainActivity
import co.edu.uniandes.misw4203.vinilos.R
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.grantLocalNetworkAccess
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.waitFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Pantalla de selección de rol (primera pantalla de la app). */
@RunWith(AndroidJUnit4::class)
class RoleSelectionE2ETest {

    @get:Rule(order = 0)
    val permissionRule = grantLocalNetworkAccess()

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    // CP-HU01-12
    @Test
    fun la_app_abre_en_la_pantalla_de_rol_con_las_dos_opciones() {
        onView(withId(R.id.visitorCard)).check(matches(isDisplayed()))
        onView(withId(R.id.collectorCard)).check(matches(isDisplayed()))
    }

    // CP-HU01-13
    @Test
    fun elegir_visitante_lleva_al_catalogo_y_muestra_el_rol() {
        onView(withId(R.id.visitorCard)).perform(click())

        waitFor { onView(withId(R.id.albumList)).check(matches(isDisplayed())) }
        onView(withId(R.id.roleChip)).check(matches(withText(R.string.role_visitor)))
    }

    // CP-HU01-14
    @Test
    fun se_puede_cambiar_de_rol_desde_la_barra_superior() {
        onView(withId(R.id.visitorCard)).perform(click())
        waitFor { onView(withId(R.id.albumList)).check(matches(isDisplayed())) }

        onView(withId(R.id.roleChip)).perform(click())
        onView(withId(R.id.collectorCard)).check(matches(isDisplayed()))

        onView(withId(R.id.collectorCard)).perform(click())
        waitFor { onView(withId(R.id.roleChip)).check(matches(withText(R.string.role_collector))) }
    }

    // CP-HU01-15
    @Test
    fun dos_toques_seguidos_en_coleccionista_abren_el_catalogo_una_sola_vez() {
        onView(isRoot()).perform(toquesSeguidos(R.id.collectorCard, R.id.collectorCard))

        waitFor { onView(withId(R.id.albumList)).check(matches(isDisplayed())) }
        onView(withId(R.id.roleChip)).check(matches(withText(R.string.role_collector)))
        verificarCatalogoSinPantallasDetras()
    }

    // CP-HU01-16
    @Test
    fun si_se_tocan_las_dos_tarjetas_seguidas_queda_el_rol_del_primer_toque() {
        onView(isRoot()).perform(toquesSeguidos(R.id.visitorCard, R.id.collectorCard))

        waitFor { onView(withId(R.id.albumList)).check(matches(isDisplayed())) }
        onView(withId(R.id.roleChip)).check(matches(withText(R.string.role_visitor)))
        verificarCatalogoSinPantallasDetras()
    }

    /**
     * Hace los toques en el mismo turno del hilo principal, así el segundo siempre llega
     * antes de que la pantalla cambie. Con doubleClick() de Espresso no está garantizado.
     */
    private fun toquesSeguidos(vararg ids: Int): ViewAction = object : ViewAction {
        override fun getConstraints() = isRoot()
        override fun getDescription() = "toques seguidos sobre las tarjetas de rol"
        override fun perform(uiController: UiController, view: View) {
            val tarjetas = ids.map { view.findViewById<View>(it) }
            tarjetas.forEach { it.performClick() }
            uiController.loopMainThreadUntilIdle()
        }
    }

    /** El destino actual es el catálogo y no quedó otra pantalla en la pila. */
    private fun verificarCatalogoSinPantallasDetras() {
        activityRule.scenario.onActivity { activity ->
            val navController = activity.findNavController(R.id.nav_host_fragment)
            assertEquals(R.id.albumListFragment, navController.currentDestination?.id)
            assertNull(navController.previousBackStackEntry)
        }
    }
}
