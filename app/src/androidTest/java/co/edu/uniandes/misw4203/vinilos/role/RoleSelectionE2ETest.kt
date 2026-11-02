package co.edu.uniandes.misw4203.vinilos.role

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import co.edu.uniandes.misw4203.vinilos.MainActivity
import co.edu.uniandes.misw4203.vinilos.R
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.grantLocalNetworkAccess
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.waitFor
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
}
