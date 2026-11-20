package co.edu.uniandes.misw4203.vinilos.accesibilidad

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import co.edu.uniandes.misw4203.vinilos.MainActivity
import co.edu.uniandes.misw4203.vinilos.R
import co.edu.uniandes.misw4203.vinilos.VinilosApplication
import co.edu.uniandes.misw4203.vinilos.utils.AccessibilityTestUtils.revisarAccesibilidad
import co.edu.uniandes.misw4203.vinilos.utils.BackendFixtures
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.abrirDetalleDeAlbum
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.grantLocalNetworkAccess
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.waitFor
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.instanceOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accesibilidad de HU05 (listado de coleccionistas) y HU11 (agregar a colección), revisada con
 * el motor del Accessibility Scanner (ATF). Backend real; para no cambiar los datos por
 * defecto, HU11 usa un álbum y un coleccionista propios con nombre único (BackendFixtures).
 */
@RunWith(AndroidJUnit4::class)
class AccesibilidadColeccionistasE2ETest {

    @get:Rule(order = 0)
    val permissionRule = grantLocalNetworkAccess()

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private lateinit var album: String
    private lateinit var coleccionista: String

    @Before
    fun preparar() {
        val sufijo = System.currentTimeMillis()
        album = "Álbum A11Y $sufijo"
        coleccionista = "Coleccionista A11Y $sufijo"
        BackendFixtures.crearAlbum(album)
        BackendFixtures.crearColeccionista(coleccionista)
        runBlocking {
            ApplicationProvider.getApplicationContext<VinilosApplication>()
                .appContainer.collectorRepository.getCollectors()
        }
    }

    private fun abrirDetalleComoColeccionista() {
        onView(withId(R.id.collectorCard)).perform(click())
        waitFor {
            onView(withId(R.id.albumList)).perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(withText(album)))
            )
        }
        abrirDetalleDeAlbum(album)
        waitFor { onView(withId(R.id.albumName)).check(matches(withText(album))) }
    }

    private fun abrirFormulario() {
        abrirDetalleComoColeccionista()
        onView(withId(R.id.addToCollectionButton)).perform(scrollTo(), click())
        waitFor { onView(withId(R.id.albumSummary)).check(matches(isDisplayed())) }
    }

    private fun elegirColeccionista() {
        onView(withId(R.id.collectorInput)).perform(scrollTo(), click())
        onData(allOf(instanceOf(String::class.java), equalTo(coleccionista)))
            .inRoot(isPlatformPopup())
            .perform(click())
    }

    private fun llenarFormulario() {
        elegirColeccionista()
        onView(withId(R.id.priceInput)).perform(scrollTo(), replaceText("35000"))
        onView(withId(R.id.statusActiveButton)).perform(scrollTo(), click())
        closeSoftKeyboard()
    }

    private fun tocarAgregar() {
        closeSoftKeyboard()
        onView(withId(R.id.saveButton)).perform(scrollTo(), click())
    }

    // A11Y-HU05-01
    @Test
    fun a11y_hu05_01_listado_de_coleccionistas_cargado() {
        onView(withId(R.id.visitorCard)).perform(click())
        onView(withId(R.id.collectorListFragment)).perform(click())
        waitFor { onView(withId(R.id.collectorList)).check(matches(hasMinimumChildCount(2))) }
        onView(isRoot()).perform(revisarAccesibilidad())
    }

    // A11Y-HU05-02 (estado vacío) y A11Y-HU05-03 (estado de error con Reintentar).
    // El backend real siempre responde, así que el contenedor de mensajes se muestra a mano:
    // lo que se revisa es el diseño de esos estados, no la lógica del ViewModel.
    private fun mostrarMensajeDelListado(titulo: Int, texto: Int, conReintentar: Boolean) {
        onView(withId(R.id.visitorCard)).perform(click())
        onView(withId(R.id.collectorListFragment)).perform(click())
        waitFor { onView(withId(R.id.collectorList)).check(matches(hasMinimumChildCount(2))) }
        activityRule.scenario.onActivity { activity ->
            activity.findViewById<View>(R.id.collectorList).visibility = View.GONE
            activity.findViewById<TextView>(R.id.messageTitle).setText(titulo)
            activity.findViewById<TextView>(R.id.messageText).setText(texto)
            activity.findViewById<View>(R.id.retryButton).visibility =
                if (conReintentar) View.VISIBLE else View.GONE
            activity.findViewById<View>(R.id.messageContainer).visibility = View.VISIBLE
        }
        onView(withId(R.id.messageContainer)).check(matches(isDisplayed()))
    }

    @Test
    fun a11y_hu05_02_estado_vacio() {
        mostrarMensajeDelListado(R.string.collectors_empty_title, R.string.collectors_empty_subtitle, false)
        onView(isRoot()).perform(revisarAccesibilidad())
    }

    @Test
    fun a11y_hu05_03_estado_de_error_con_reintentar() {
        mostrarMensajeDelListado(R.string.collectors_error_title, R.string.collectors_error_default, true)
        onView(isRoot()).perform(revisarAccesibilidad())
    }

    // A11Y-HU11-01
    @Test
    fun a11y_hu11_01_detalle_del_album_con_el_boton_agregar_a_coleccion() {
        abrirDetalleComoColeccionista()
        onView(withId(R.id.addToCollectionButton)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(isRoot()).perform(revisarAccesibilidad())
    }

    // A11Y-HU11-02
    @Test
    fun a11y_hu11_02_formulario_vacio() {
        abrirFormulario()
        onView(isRoot()).perform(revisarAccesibilidad())
    }

    // A11Y-HU11-02 (etiquetas): sin depender de TalkBack, el campo debe quedar ligado a su
    // etiqueta (labelFor, WCAG 1.3.1) y la etiqueta debe leerse "obligatorio" y sin asterisco.
    // getLabeledBy() solo funciona en un nodo "sellado" (el que entrega el servicio de
    // accesibilidad), no en el que devuelve createAccessibilityNodeInfo() de la vista. Por eso
    // el nodo se pide a UiAutomation, fuera del hilo principal para no bloquearlo.
    private fun verificarEtiquetaDelCampo(idCampo: Int, descripcion: String) {
        onView(withId(idCampo)).perform(scrollTo()).check(matches(isDisplayed()))
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val nombre = instrumentation.targetContext.resources.getResourceEntryName(idCampo)
        val idCompleto = "${instrumentation.targetContext.packageName}:id/$nombre"
        val raiz = instrumentation.uiAutomation.rootInActiveWindow
        val campo = raiz.findAccessibilityNodeInfosByViewId(idCompleto).firstOrNull()
        assertNotNull("No se encontró el campo $nombre", campo)
        val etiqueta = campo!!.labeledBy
        assertNotNull("El campo $nombre no tiene etiqueta asociada (labelFor)", etiqueta)
        assertEquals(descripcion, etiqueta!!.contentDescription?.toString())
    }

    @Test
    fun a11y_hu11_02_coleccionista_y_precio_estan_ligados_a_su_etiqueta() {
        abrirFormulario()
        verificarEtiquetaDelCampo(R.id.collectorInput, "Coleccionista, obligatorio")
        verificarEtiquetaDelCampo(R.id.priceInput, "Precio, obligatorio")
    }

    // A11Y-HU11-03
    @Test
    fun a11y_hu11_03_formulario_con_errores_de_validacion() {
        abrirFormulario()
        tocarAgregar()
        waitFor { onView(withId(R.id.errorBanner)).check(matches(isDisplayed())) }
        onView(isRoot()).perform(revisarAccesibilidad())
    }

    // A11Y-HU11-04
    @Test
    fun a11y_hu11_04_formulario_con_el_error_de_duplicado() {
        abrirFormulario()
        llenarFormulario()
        tocarAgregar()
        waitFor {
            onView(withId(R.id.topBarTitle)).check(matches(withText(R.string.album_detail_title)))
        }
        onView(withId(R.id.addToCollectionButton)).perform(scrollTo(), click())
        llenarFormulario()
        tocarAgregar()
        waitFor {
            onView(withText("Este álbum ya está en la colección de $coleccionista"))
                .check(matches(isDisplayed()))
        }
        onView(isRoot()).perform(revisarAccesibilidad())
    }

    // A11Y-HU11-05
    @Test
    fun a11y_hu11_05_formulario_con_datos_validos_y_estado_elegido() {
        abrirFormulario()
        llenarFormulario()
        onView(isRoot()).perform(revisarAccesibilidad())
    }
}
