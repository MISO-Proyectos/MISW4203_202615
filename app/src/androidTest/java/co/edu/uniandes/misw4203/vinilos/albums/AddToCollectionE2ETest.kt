package co.edu.uniandes.misw4203.vinilos.albums

import android.content.pm.ActivityInfo
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import co.edu.uniandes.misw4203.vinilos.MainActivity
import co.edu.uniandes.misw4203.vinilos.R
import co.edu.uniandes.misw4203.vinilos.VinilosApplication
import co.edu.uniandes.misw4203.vinilos.utils.BackendFixtures
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.abrirDetalleDeAlbum
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.clicDirecto
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.conError
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.grantLocalNetworkAccess
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.sinError
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.waitFor
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.instanceOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * HU11 - Agregar álbum a coleccionista (E2E con el backend real).
 * Precondición: backend BackVynils levantado. Para no cambiar los datos por defecto que usan
 * las pruebas de HU02, HU05 y HU06, cada prueba crea por la API su propio álbum y su propio
 * coleccionista con nombre único (BackendFixtures) y solo usa esos dos.
 */
@RunWith(AndroidJUnit4::class)
class AddToCollectionE2ETest {

    @get:Rule(order = 0)
    val permissionRule = grantLocalNetworkAccess()

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private companion object {
        const val TITULO = "Agregar a colección"
        const val ERROR_COLECCIONISTA = "Selecciona un coleccionista"
        const val ERROR_PRECIO = "Ingresa un precio válido"
        const val PRECIO = "35000"
    }

    private lateinit var album: String
    private lateinit var coleccionista: String

    @Before
    fun preparar() {
        val sufijo = System.currentTimeMillis()
        album = "Álbum HU11 E2E $sufijo"
        coleccionista = "Coleccionista HU11 E2E $sufijo"
        BackendFixtures.crearAlbum(album)
        BackendFixtures.crearColeccionista(coleccionista)
        // Se deja el listado de coleccionistas en memoria para que el campo ya lo tenga al abrir el formulario
        runBlocking {
            ApplicationProvider.getApplicationContext<VinilosApplication>()
                .appContainer.collectorRepository.getCollectors()
        }
    }

    private fun abrirDetalle(rol: Int) {
        onView(withId(rol)).perform(click())
        // El álbum de prueba queda al final del catálogo: primero se desplaza hasta él
        waitFor {
            onView(withId(R.id.albumList)).perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(withText(album)))
            )
        }
        abrirDetalleDeAlbum(album)
        waitFor { onView(withId(R.id.albumName)).check(matches(withText(album))) }
    }

    private fun tocarAgregarAColeccion() {
        onView(withId(R.id.addToCollectionButton)).perform(scrollTo(), click())
        onView(withId(R.id.topBarTitle)).check(matches(withText(TITULO)))
    }

    private fun abrirFormulario() {
        abrirDetalle(R.id.collectorCard)
        tocarAgregarAColeccion()
    }

    /** Abre la lista del campo Coleccionista y toca el nombre; onData se desplaza si la lista es larga. */
    private fun elegirColeccionista() {
        onView(withId(R.id.collectorInput)).perform(scrollTo(), click())
        onData(allOf(instanceOf(String::class.java), equalTo(coleccionista)))
            .inRoot(isPlatformPopup())
            .perform(click())
    }

    private fun llenarFormulario(precio: String = PRECIO) {
        elegirColeccionista()
        onView(withId(R.id.priceInput)).perform(scrollTo(), replaceText(precio))
        onView(withId(R.id.statusActiveButton)).perform(scrollTo(), click())
    }

    private fun tocarAgregar() {
        closeSoftKeyboard()
        onView(withId(R.id.saveButton)).perform(scrollTo(), click())
    }

    private fun volvioAlDetalle() {
        waitFor { onView(withId(R.id.topBarTitle)).check(matches(withText(R.string.album_detail_title))) }
        onView(withId(R.id.albumName)).check(matches(withText(album)))
    }

    // CP-HU11-01
    @Test
    fun visitante_no_ve_el_boton_agregar_a_coleccion() {
        abrirDetalle(R.id.visitorCard)

        onView(withId(R.id.addToCollectionButton))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    // CP-HU11-02
    @Test
    fun coleccionista_abre_el_formulario_con_la_tarjeta_y_los_tres_campos() {
        abrirFormulario()

        waitFor { onView(withId(R.id.albumSummary)).check(matches(isDisplayed())) }
        onView(withId(R.id.albumSummary)).check(matches(hasDescendant(withText(album))))
        onView(withId(R.id.albumInfo)).check(matches(withText(containsString("track"))))
        onView(withId(R.id.collectorInput)).check(matches(isDisplayed()))
        onView(withId(R.id.priceInput)).check(matches(isDisplayed()))
        onView(withId(R.id.statusActiveButton)).check(matches(isDisplayed()))
        onView(withId(R.id.statusInactiveButton)).check(matches(isDisplayed()))
        onView(withId(R.id.saveButton)).check(matches(isDisplayed()))
        onView(withId(R.id.errorBanner)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        // La barra superior muestra la "X" para cerrar
        onView(withContentDescription(R.string.close)).check(matches(isDisplayed()))
    }

    // CP-HU11-03
    @Test
    fun guardar_vacio_muestra_el_aviso_y_los_tres_errores() {
        abrirFormulario()

        tocarAgregar()

        onView(withId(R.id.errorBanner)).check(matches(isDisplayed()))
        onView(withId(R.id.collectorLayout)).check(matches(conError(ERROR_COLECCIONISTA)))
        onView(withId(R.id.priceLayout)).check(matches(conError(ERROR_PRECIO)))
        onView(withId(R.id.statusError)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withId(R.id.statusError)).check(matches(withText("Selecciona el estado")))
        onView(withId(R.id.topBarTitle)).check(matches(withText(TITULO)))
    }

    // CP-HU11-04
    @Test
    fun un_precio_cero_muestra_ingresa_un_precio_valido() {
        abrirFormulario()

        llenarFormulario(precio = "0")
        tocarAgregar()

        onView(withId(R.id.priceLayout)).check(matches(conError(ERROR_PRECIO)))
        onView(withId(R.id.collectorLayout)).check(matches(sinError()))
        onView(withId(R.id.topBarTitle)).check(matches(withText(TITULO)))
    }

    // CP-HU11-05
    @Test
    fun corregir_los_campos_quita_los_errores_y_el_aviso() {
        abrirFormulario()
        tocarAgregar()
        onView(withId(R.id.errorBanner)).check(matches(isDisplayed()))

        llenarFormulario()

        onView(withId(R.id.collectorLayout)).check(matches(sinError()))
        onView(withId(R.id.priceLayout)).check(matches(sinError()))
        onView(withId(R.id.statusError)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.errorBanner)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    // CP-HU11-06
    @Test
    fun agregar_con_datos_validos_vuelve_al_detalle_con_el_aviso() {
        abrirFormulario()

        llenarFormulario()
        tocarAgregar()

        waitFor {
            onView(withText("$album se agregó a la colección de $coleccionista"))
                .check(matches(isDisplayed()))
        }
        volvioAlDetalle()
    }

    // CP-HU11-07
    @Test
    fun repetir_con_el_mismo_coleccionista_avisa_que_ya_esta_en_la_coleccion() {
        abrirFormulario()
        llenarFormulario()
        tocarAgregar()
        volvioAlDetalle()

        tocarAgregarAColeccion()
        llenarFormulario()
        tocarAgregar()

        waitFor {
            onView(withId(R.id.collectorLayout))
                .check(matches(conError("Este álbum ya está en la colección de $coleccionista")))
        }
        onView(withId(R.id.topBarTitle)).check(matches(withText(TITULO)))
    }

    // CP-HU11-08
    @Test
    fun el_detalle_del_coleccionista_muestra_el_album_con_su_precio_y_estado() {
        abrirFormulario()
        llenarFormulario()
        tocarAgregar()
        volvioAlDetalle()

        // Atrás al catálogo (pantalla principal) y de ahí a Coleccionistas. El aviso del detalle
        // sigue visible sobre la barra inferior hasta que se cierra (la barra no existe en el
        // detalle, no se puede anclar el aviso a ella): se espera a que desaparezca, porque un
        // clic sobre "Coleccionistas" mientras está visible le caería al aviso.
        pressBack()
        waitFor(timeoutMs = 15_000) {
            onView(withId(com.google.android.material.R.id.snackbar_text)).check(doesNotExist())
        }
        waitFor { onView(withId(R.id.collectorListFragment)).perform(click()) }
        waitFor {
            onView(withId(R.id.collectorList)).perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(withText(coleccionista)))
            )
        }
        onView(withId(R.id.collectorList)).perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(coleccionista)),
                click()
            )
        )

        waitFor { onView(withText(album)).perform(scrollTo()).check(matches(isDisplayed())) }
        onView(withText("Precio: $PRECIO")).check(matches(isDisplayed()))
        onView(withText("Activo")).check(matches(isDisplayed()))
    }

    // CP-HU11-09
    @Test
    fun cancelar_y_la_x_vuelven_al_detalle() {
        abrirFormulario()
        closeSoftKeyboard()
        onView(withId(R.id.cancelButton)).perform(scrollTo(), click())
        volvioAlDetalle()

        tocarAgregarAColeccion()
        onView(withContentDescription(R.string.close)).perform(clicDirecto())
        volvioAlDetalle()
    }

    // CP-HU11-10
    @Test
    fun al_rotar_se_conservan_coleccionista_precio_y_estado() {
        abrirFormulario()
        llenarFormulario()
        closeSoftKeyboard()

        activityRule.scenario.onActivity {
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        try {
            waitFor {
                onView(withId(R.id.collectorInput)).perform(scrollTo()).check(matches(withText(coleccionista)))
                onView(withId(R.id.priceInput)).perform(scrollTo()).check(matches(withText(PRECIO)))
                onView(withId(R.id.statusActiveButton)).perform(scrollTo()).check(matches(isChecked()))
            }
            onView(withId(R.id.statusInactiveButton)).check(matches(androidx.test.espresso.matcher.ViewMatchers.isNotChecked()))
        } finally {
            activityRule.scenario.onActivity {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }
}
