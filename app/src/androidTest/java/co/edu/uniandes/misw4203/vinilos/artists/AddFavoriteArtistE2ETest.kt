package co.edu.uniandes.misw4203.vinilos.artists

import android.content.pm.ActivityInfo
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
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
 * HU10 - Agregar un artista a favoritos (E2E con el backend real).
 * Precondición: backend BackVynils levantado con sus datos por defecto (Rubén Blades, músico
 * id 100, y Queen, banda id 101). Para no cambiar los favoritos de los coleccionistas por defecto
 * que revisa HU06, cada prueba crea por la API su propio coleccionista con nombre único
 * (BackendFixtures) y solo registra favoritos a ese coleccionista.
 */
@RunWith(AndroidJUnit4::class)
class AddFavoriteArtistE2ETest {

    @get:Rule(order = 0)
    val permissionRule = grantLocalNetworkAccess()

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private companion object {
        const val TITULO = "Agregar a favoritos"
        const val MUSICO = "Rubén Blades Bellido de Luna"
        const val BANDA = "Queen"
        const val ERROR_COLECCIONISTA = "Selecciona un coleccionista"
        const val AVISO_OBLIGATORIOS = "Completa los campos obligatorios para guardar."
        const val AYUDA = "El artista quedará en los favoritos de este coleccionista y se verá en su detalle."
    }

    private lateinit var coleccionista: String

    @Before
    fun preparar() {
        coleccionista = "Coleccionista HU10 E2E ${System.currentTimeMillis()}"
        BackendFixtures.crearColeccionista(coleccionista)
        // Se deja el listado de coleccionistas en memoria para que el campo ya lo tenga al abrir el formulario
        runBlocking {
            ApplicationProvider.getApplicationContext<VinilosApplication>()
                .appContainer.collectorRepository.getCollectors()
        }
    }

    /** Entra con el rol dado (visitorCard o collectorCard), abre Artistas y el detalle del artista. */
    private fun abrirDetalleDeArtista(rol: Int, nombre: String) {
        onView(withId(rol)).perform(click())
        onView(withId(R.id.artistListFragment)).perform(click())
        waitFor { onView(withId(R.id.artistList)).check(matches(hasDescendant(withText(nombre)))) }
        onView(withId(R.id.artistList)).perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(nombre)),
                click()
            )
        )
        waitFor { onView(withId(R.id.artistName)).check(matches(withText(nombre))) }
    }

    private fun tocarAgregarAFavoritos() {
        waitFor { onView(withId(R.id.addFavoriteButton)).check(matches(isDisplayed())) }
        onView(withId(R.id.addFavoriteButton)).perform(scrollTo(), click())
        onView(withId(R.id.topBarTitle)).check(matches(withText(TITULO)))
    }

    private fun abrirFormulario(nombre: String = MUSICO) {
        abrirDetalleDeArtista(R.id.collectorCard, nombre)
        tocarAgregarAFavoritos()
    }

    /** Abre la lista del campo Coleccionista y toca el nombre; onData se desplaza si la lista es larga. */
    private fun elegirColeccionista() {
        onView(withId(R.id.collectorInput)).perform(scrollTo(), click())
        onData(allOf(instanceOf(String::class.java), equalTo(coleccionista)))
            .inRoot(isPlatformPopup())
            .perform(click())
    }

    private fun tocarAgregar() {
        closeSoftKeyboard()
        onView(withId(R.id.saveButton)).perform(scrollTo(), click())
    }

    private fun volvioAlDetalle(nombre: String = MUSICO) {
        waitFor { onView(withId(R.id.topBarTitle)).check(matches(withText(R.string.artist_detail_title))) }
        onView(withId(R.id.artistName)).check(matches(withText(nombre)))
    }

    /** Desde el detalle del artista vuelve al listado, abre Coleccionistas y entra al detalle del coleccionista de prueba. */
    private fun abrirDetalleDelColeccionista() {
        pressBack()
        waitFor { onView(withId(R.id.artistList)).check(matches(isDisplayed())) }
        // El toque se repite hasta que la lista de coleccionistas aparece: si cae durante la transición de volver, se pierde
        waitFor {
            onView(withId(R.id.collectorListFragment)).perform(click())
            onView(withId(R.id.collectorList)).check(matches(isDisplayed()))
        }
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
    }

    // CP-HU10-01
    @Test
    fun visitante_no_ve_el_boton_agregar_a_favoritos() {
        abrirDetalleDeArtista(R.id.visitorCard, MUSICO)

        onView(withId(R.id.addFavoriteButton))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    // CP-HU10-02
    @Test
    fun coleccionista_ve_el_boton_y_abre_el_formulario_con_la_tarjeta_del_artista() {
        abrirFormulario()

        onView(withId(R.id.artistSummary)).check(matches(isDisplayed()))
        onView(withId(R.id.artistName)).check(matches(withText(MUSICO)))
        onView(withId(R.id.artistInfo)).check(matches(withText(containsString("Músico · "))))
        onView(withId(R.id.artistInfo)).check(matches(withText(containsString("álbum"))))
        onView(withText("Los campos marcados con * son obligatorios.")).check(matches(isDisplayed()))
        onView(withId(R.id.collectorInput)).check(matches(isDisplayed()))
        onView(withText(AYUDA)).check(matches(isDisplayed()))
        onView(withId(R.id.saveButton)).check(matches(isDisplayed()))
        onView(withId(R.id.cancelButton)).check(matches(isDisplayed()))
        onView(withId(R.id.errorBanner)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        // La barra superior muestra la "X" para cerrar
        onView(withContentDescription(R.string.close)).check(matches(isDisplayed()))
    }

    // CP-HU10-03
    @Test
    fun guardar_sin_coleccionista_muestra_el_aviso_y_el_error_del_campo() {
        abrirFormulario()

        tocarAgregar()

        onView(withId(R.id.errorBanner)).check(matches(isDisplayed()))
        onView(withText(AVISO_OBLIGATORIOS)).check(matches(isDisplayed()))
        onView(withId(R.id.collectorLayout)).check(matches(conError(ERROR_COLECCIONISTA)))
        onView(withId(R.id.topBarTitle)).check(matches(withText(TITULO)))
    }

    // CP-HU10-04
    @Test
    fun elegir_el_coleccionista_quita_el_error_y_el_aviso() {
        abrirFormulario()
        tocarAgregar()
        onView(withId(R.id.errorBanner)).check(matches(isDisplayed()))

        elegirColeccionista()

        onView(withId(R.id.collectorLayout)).check(matches(sinError()))
        onView(withId(R.id.errorBanner)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    // CP-HU10-05
    @Test
    fun registrar_un_musico_vuelve_al_detalle_con_el_mensaje_de_exito() {
        abrirFormulario()

        elegirColeccionista()
        tocarAgregar()

        waitFor {
            onView(withText("$MUSICO ahora es favorito de $coleccionista"))
                .check(matches(isDisplayed()))
        }
        volvioAlDetalle()
    }

    // CP-HU10-06
    @Test
    fun el_musico_registrado_aparece_en_los_favoritos_del_coleccionista() {
        abrirFormulario()
        elegirColeccionista()
        tocarAgregar()
        volvioAlDetalle()

        abrirDetalleDelColeccionista()

        waitFor { onView(withText(MUSICO)).perform(scrollTo()).check(matches(isDisplayed())) }
    }

    // CP-HU10-07
    @Test
    fun registrar_una_banda_la_deja_en_los_favoritos_del_coleccionista() {
        abrirFormulario(BANDA)
        elegirColeccionista()
        tocarAgregar()

        waitFor {
            onView(withText("$BANDA ahora es favorito de $coleccionista"))
                .check(matches(isDisplayed()))
        }
        volvioAlDetalle(BANDA)

        abrirDetalleDelColeccionista()
        waitFor { onView(withText(BANDA)).perform(scrollTo()).check(matches(isDisplayed())) }
    }

    // CP-HU10-08
    @Test
    fun repetir_con_el_mismo_coleccionista_avisa_que_ya_es_favorito() {
        abrirFormulario()
        elegirColeccionista()
        tocarAgregar()
        volvioAlDetalle()

        tocarAgregarAFavoritos()
        elegirColeccionista()
        tocarAgregar()

        waitFor {
            onView(withId(R.id.collectorLayout))
                .check(matches(conError("Este artista ya es favorito de $coleccionista")))
        }
        onView(withId(R.id.topBarTitle)).check(matches(withText(TITULO)))
    }

    // CP-HU10-09
    @Test
    fun cancelar_y_la_x_vuelven_al_detalle_del_artista() {
        abrirFormulario()
        closeSoftKeyboard()
        onView(withId(R.id.cancelButton)).perform(scrollTo(), click())
        volvioAlDetalle()

        tocarAgregarAFavoritos()
        onView(withContentDescription(R.string.close)).perform(clicDirecto())
        volvioAlDetalle()
    }

    // CP-HU10-10
    @Test
    fun al_rotar_se_conserva_el_coleccionista_elegido() {
        abrirFormulario()
        elegirColeccionista()
        closeSoftKeyboard()

        activityRule.scenario.onActivity {
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        try {
            waitFor {
                onView(withId(R.id.collectorInput)).perform(scrollTo()).check(matches(withText(coleccionista)))
            }
            onView(withId(R.id.artistName)).check(matches(withText(MUSICO)))
        } finally {
            activityRule.scenario.onActivity {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }
}
