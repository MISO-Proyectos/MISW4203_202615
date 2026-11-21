package co.edu.uniandes.misw4203.vinilos.accesibilidad

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import co.edu.uniandes.misw4203.vinilos.MainActivity
import co.edu.uniandes.misw4203.vinilos.R
import co.edu.uniandes.misw4203.vinilos.VinilosApplication
import co.edu.uniandes.misw4203.vinilos.utils.AccessibilityTestUtils.revisarAccesibilidad
import co.edu.uniandes.misw4203.vinilos.utils.BackendFixtures
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.grantLocalNetworkAccess
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.waitFor
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.instanceOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accesibilidad de HU10 (agregar un artista a favoritos): botón del detalle del artista y
 * formulario, revisados con el motor del Accessibility Scanner (ATF), más comprobaciones
 * directas de A11Y-27, A11Y-28 y A11Y-29. Backend real; el coleccionista se crea por la API con
 * nombre único (BackendFixtures) para no cambiar los datos por defecto.
 */
@RunWith(AndroidJUnit4::class)
class AccesibilidadFavoritosE2ETest {

    @get:Rule(order = 0)
    val permissionRule = grantLocalNetworkAccess()

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private companion object {
        const val MUSICO = "Rubén Blades Bellido de Luna"
    }

    private lateinit var coleccionista: String

    @Before
    fun preparar() {
        coleccionista = "Coleccionista A11Y HU10 ${System.currentTimeMillis()}"
        BackendFixtures.crearColeccionista(coleccionista)
        runBlocking {
            ApplicationProvider.getApplicationContext<VinilosApplication>()
                .appContainer.collectorRepository.getCollectors()
        }
    }

    private fun abrirDetalleDelArtista() {
        onView(withId(R.id.collectorCard)).perform(click())
        onView(withId(R.id.artistListFragment)).perform(click())
        waitFor { onView(withId(R.id.artistList)).check(matches(hasDescendant(withText(MUSICO)))) }
        onView(withId(R.id.artistList)).perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(MUSICO)),
                click()
            )
        )
        waitFor { onView(withId(R.id.artistName)).check(matches(withText(MUSICO))) }
    }

    private fun abrirFormulario() {
        abrirDetalleDelArtista()
        waitFor { onView(withId(R.id.addFavoriteButton)).check(matches(isDisplayed())) }
        onView(withId(R.id.addFavoriteButton)).perform(scrollTo(), click())
        waitFor { onView(withId(R.id.artistSummary)).check(matches(isDisplayed())) }
    }

    private fun elegirColeccionista() {
        onView(withId(R.id.collectorInput)).perform(scrollTo(), click())
        onData(allOf(instanceOf(String::class.java), equalTo(coleccionista)))
            .inRoot(isPlatformPopup())
            .perform(click())
        closeSoftKeyboard()
    }

    // A11Y-HU10-01
    @Test
    fun a11y_hu10_01_detalle_del_artista_con_el_boton_agregar_a_favoritos() {
        abrirDetalleDelArtista()
        waitFor { onView(withId(R.id.addFavoriteButton)).check(matches(isDisplayed())) }
        onView(isRoot()).perform(revisarAccesibilidad())
    }

    // A11Y-HU10-02
    @Test
    fun a11y_hu10_02_formulario_vacio() {
        abrirFormulario()
        onView(isRoot()).perform(revisarAccesibilidad())
    }

    // A11Y-HU10-03
    @Test
    fun a11y_hu10_03_formulario_con_el_error_de_validacion() {
        abrirFormulario()
        closeSoftKeyboard()
        onView(withId(R.id.saveButton)).perform(scrollTo(), click())
        waitFor { onView(withId(R.id.errorBanner)).check(matches(isDisplayed())) }
        onView(isRoot()).perform(revisarAccesibilidad())
    }

    // A11Y-HU10-04
    @Test
    fun a11y_hu10_04_formulario_con_coleccionista_elegido() {
        abrirFormulario()
        elegirColeccionista()
        onView(isRoot()).perform(revisarAccesibilidad())
    }

    // A11Y-27 (C5): el botón crece con la fuente, no tiene alto fijo
    @Test
    fun a11y_27_el_boton_guardar_no_tiene_alto_fijo() {
        abrirFormulario()
        activityRule.scenario.onActivity { activity ->
            val boton = activity.findViewById<View>(R.id.saveButton)
            val minimo = (56 * activity.resources.displayMetrics.density).toInt()
            assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, boton.layoutParams.height)
            assertTrue("El alto mínimo debe ser de 56dp", boton.minimumHeight >= minimo)
        }
    }

    // A11Y-28 (C7): el indicador de envío se anuncia al aparecer
    @Test
    fun a11y_28_el_indicador_de_envio_es_una_live_region() {
        abrirFormulario()
        activityRule.scenario.onActivity { activity ->
            val indicador = activity.findViewById<View>(R.id.savingIndicator)
            assertEquals(
                ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE,
                ViewCompat.getAccessibilityLiveRegion(indicador)
            )
        }
    }

    // A11Y-29 (C7): el aviso de error se lee como un solo elemento
    @Test
    fun a11y_29_el_aviso_de_error_es_un_solo_elemento_para_el_lector() {
        abrirFormulario()
        activityRule.scenario.onActivity { activity ->
            val aviso = activity.findViewById<View>(R.id.errorBanner)
            assertTrue(ViewCompat.isScreenReaderFocusable(aviso))
            assertEquals(
                ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE,
                ViewCompat.getAccessibilityLiveRegion(aviso)
            )
        }
    }
}
