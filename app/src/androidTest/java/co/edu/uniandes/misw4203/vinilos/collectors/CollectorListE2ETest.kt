package co.edu.uniandes.misw4203.vinilos.collectors

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import co.edu.uniandes.misw4203.vinilos.MainActivity
import co.edu.uniandes.misw4203.vinilos.R
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.grantLocalNetworkAccess
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.waitFor
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * HU05 - Consultar el listado de coleccionistas (E2E con el backend real).
 * Precondición: backend BackVynils recién levantado con su base vacía; al arrancar
 * carga solo 2 coleccionistas por defecto (ids 100 y 101) que usan estas pruebas.
 */
@RunWith(AndroidJUnit4::class)
class CollectorListE2ETest {

    @get:Rule(order = 0)
    val permissionRule = grantLocalNetworkAccess()

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    // La app abre en la pantalla de rol: se entra como Visitante y se abre la sección Coleccionistas.
    @Before
    fun entrar_como_visitante_y_abrir_coleccionistas() {
        onView(withId(R.id.visitorCard)).perform(click())
        onView(withId(R.id.collectorListFragment)).perform(click())
    }

    private companion object {
        const val PRIMER_COLECCIONISTA = "Manolo Bellon"
        const val SEGUNDO_COLECCIONISTA = "Jaime Monsalve"
        const val INICIALES_PRIMERO = "MB"
        const val CORREO_PRIMERO = "manollo@caracol.com.co"
        const val TELEFONO_PRIMERO = "3502457896"
        const val CONTADOR = "2 coleccionistas"
    }

    private fun laListaMuestra(texto: String) = waitFor {
        onView(withId(R.id.collectorList)).check(matches(hasDescendant(withText(texto))))
    }

    // CP-HU05-01
    @Test
    fun listado_muestra_al_menos_un_coleccionista() {
        waitFor { onView(withId(R.id.collectorList)).check(matches(isDisplayed())) }
        waitFor { onView(withId(R.id.collectorList)).check(matches(hasMinimumChildCount(1))) }
    }

    // CP-HU05-02
    @Test
    fun listado_muestra_los_nombres_y_los_datos_de_cada_tarjeta() {
        laListaMuestra(PRIMER_COLECCIONISTA)
        laListaMuestra(SEGUNDO_COLECCIONISTA)
        laListaMuestra(INICIALES_PRIMERO)
        laListaMuestra(CORREO_PRIMERO)
        laListaMuestra(TELEFONO_PRIMERO)
    }

    // CP-HU05-03 (con la HU06, tocar un coleccionista abre su detalle)
    @Test
    fun tocar_un_coleccionista_abre_su_detalle() {
        laListaMuestra(PRIMER_COLECCIONISTA)
        onView(withId(R.id.collectorList)).perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(PRIMER_COLECCIONISTA)),
                click()
            )
        )
        waitFor {
            onView(withId(R.id.detailName)).check(matches(withText(PRIMER_COLECCIONISTA)))
        }
        onView(withId(R.id.topBarTitle)).check(matches(withText(R.string.collector_detail_title)))
    }

    // CP-HU05-04
    @Test
    fun listado_muestra_el_titulo_de_la_pantalla() {
        onView(withId(R.id.topBarTitle)).check(matches(withText(R.string.nav_collectors)))
    }

    // CP-HU05-09
    @Test
    fun listado_muestra_el_contador_de_coleccionistas() {
        laListaMuestra(CONTADOR)
    }
}
