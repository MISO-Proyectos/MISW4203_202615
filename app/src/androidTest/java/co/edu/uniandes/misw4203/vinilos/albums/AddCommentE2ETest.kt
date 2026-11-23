package co.edu.uniandes.misw4203.vinilos.albums

import android.content.pm.ActivityInfo
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
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
 * HU09 - Comentar un álbum (E2E con el backend real).
 * Precondición: backend BackVynils levantado. Para no cambiar los datos por defecto que usan
 * las pruebas de HU02 y HU06 (cuentan comentarios, tracks y álbumes), cada prueba crea por la
 * API su propio álbum y su propio coleccionista con nombre único (BackendFixtures) y solo usa
 * esos dos.
 */
@RunWith(AndroidJUnit4::class)
class AddCommentE2ETest {

    @get:Rule(order = 0)
    val permissionRule = grantLocalNetworkAccess()

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private companion object {
        const val TITULO = "Comentar álbum"
        const val ERROR_COLECCIONISTA = "Selecciona un coleccionista"
        const val ERROR_CALIFICACION = "Elige una calificación"
        const val ERROR_COMENTARIO = "Escribe tu comentario"
        const val CALIFICACION = 4
    }

    private lateinit var album: String
    private lateinit var coleccionista: String
    private lateinit var comentario: String

    @Before
    fun preparar() {
        val sufijo = System.currentTimeMillis()
        album = "Álbum HU09 E2E $sufijo"
        coleccionista = "Coleccionista HU09 E2E $sufijo"
        comentario = "Comentario de prueba E2E $sufijo"
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

    private fun abrirFormulario() {
        abrirDetalle(R.id.collectorCard)
        onView(withId(R.id.addCommentButton)).perform(scrollTo(), click())
        onView(withId(R.id.topBarTitle)).check(matches(withText(TITULO)))
    }

    /** Abre la lista del campo Coleccionista y toca el nombre; onData se desplaza si la lista es larga. */
    private fun elegirColeccionista() {
        onView(withId(R.id.collectorInput)).perform(scrollTo(), click())
        onData(allOf(instanceOf(String::class.java), equalTo(coleccionista)))
            .inRoot(isPlatformPopup())
            .perform(click())
    }

    /** Toca la estrella [estrellas] del RatingBar, como lo haría el usuario con el dedo. */
    private fun tocarEstrella(estrellas: Int) {
        val tocar = GeneralClickAction(
            Tap.SINGLE,
            { view ->
                val posicion = IntArray(2).also(view::getLocationOnScreen)
                floatArrayOf(
                    posicion[0] + view.width * (estrellas - 0.5f) / 5f,
                    posicion[1] + view.height / 2f
                )
            },
            Press.FINGER,
            0,
            0
        )
        onView(withId(R.id.ratingBar)).perform(scrollTo(), tocar)
    }

    private fun llenarFormulario() {
        elegirColeccionista()
        tocarEstrella(CALIFICACION)
        onView(withId(R.id.descriptionInput)).perform(scrollTo(), replaceText(comentario))
    }

    private fun tocarPublicar() {
        closeSoftKeyboard()
        onView(withId(R.id.saveButton)).perform(scrollTo(), click())
    }

    private fun volvioAlDetalle() {
        waitFor { onView(withId(R.id.topBarTitle)).check(matches(withText(R.string.album_detail_title))) }
        onView(withId(R.id.albumName)).check(matches(withText(album)))
    }

    // CP-HU09-02
    @Test
    fun visitante_no_ve_el_boton_comentar() {
        abrirDetalle(R.id.visitorCard)

        onView(withId(R.id.addCommentButton))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    // CP-HU09-03
    @Test
    fun coleccionista_abre_el_formulario_con_la_tarjeta_y_los_tres_campos() {
        abrirFormulario()

        waitFor { onView(withId(R.id.albumSummary)).check(matches(isDisplayed())) }
        onView(withId(R.id.albumSummary)).check(matches(hasDescendant(withText(album))))
        onView(withId(R.id.albumInfo)).check(matches(withText(containsString("track"))))
        onView(withId(R.id.collectorInput)).check(matches(isDisplayed()))
        onView(withId(R.id.ratingBar)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withId(R.id.descriptionInput)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withId(R.id.saveButton)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withId(R.id.errorBanner)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        // La barra superior muestra la "X" para cerrar
        onView(withContentDescription(R.string.close)).check(matches(isDisplayed()))
    }

    // CP-HU09-04
    @Test
    fun publicar_vacio_muestra_el_aviso_y_los_tres_errores() {
        abrirFormulario()

        tocarPublicar()

        onView(withId(R.id.errorBanner)).check(matches(isDisplayed()))
        onView(withId(R.id.collectorLayout)).check(matches(conError(ERROR_COLECCIONISTA)))
        onView(withId(R.id.ratingError)).perform(scrollTo()).check(matches(withText(ERROR_CALIFICACION)))
        onView(withId(R.id.descriptionLayout)).perform(scrollTo()).check(matches(conError(ERROR_COMENTARIO)))
        onView(withId(R.id.topBarTitle)).check(matches(withText(TITULO)))
    }

    // CP-HU09-05
    @Test
    fun corregir_los_campos_quita_los_errores_y_el_aviso() {
        abrirFormulario()
        tocarPublicar()
        onView(withId(R.id.errorBanner)).check(matches(isDisplayed()))

        llenarFormulario()

        onView(withId(R.id.collectorLayout)).check(matches(sinError()))
        onView(withId(R.id.ratingError)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.descriptionLayout)).check(matches(sinError()))
        onView(withId(R.id.errorBanner)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    // CP-HU09-06
    @Test
    fun publicar_con_datos_validos_vuelve_al_detalle_con_el_comentario_y_el_aviso() {
        abrirFormulario()

        llenarFormulario()
        tocarPublicar()

        waitFor { onView(withText("Comentario publicado")).check(matches(isDisplayed())) }
        volvioAlDetalle()
        waitFor { onView(withText(comentario)).perform(scrollTo()).check(matches(isDisplayed())) }
    }

    // CP-HU09-07
    @Test
    fun cancelar_y_la_x_vuelven_al_detalle_sin_publicar() {
        abrirFormulario()
        closeSoftKeyboard()
        onView(withId(R.id.cancelButton)).perform(scrollTo(), click())
        volvioAlDetalle()
        onView(withId(R.id.commentsEmpty)).perform(scrollTo()).check(matches(isDisplayed()))

        onView(withId(R.id.addCommentButton)).perform(scrollTo(), click())
        onView(withId(R.id.topBarTitle)).check(matches(withText(TITULO)))
        onView(withContentDescription(R.string.close)).perform(clicDirecto())
        volvioAlDetalle()
        onView(withId(R.id.commentsEmpty)).perform(scrollTo()).check(matches(isDisplayed()))
    }

    // CP-HU09-08
    @Test
    fun al_rotar_se_conservan_coleccionista_calificacion_y_comentario() {
        abrirFormulario()
        llenarFormulario()
        closeSoftKeyboard()

        activityRule.scenario.onActivity {
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        try {
            waitFor {
                onView(withId(R.id.collectorInput)).perform(scrollTo()).check(matches(withText(coleccionista)))
                onView(withId(R.id.descriptionInput)).perform(scrollTo()).check(matches(withText(comentario)))
                onView(withId(R.id.ratingBar)).perform(scrollTo())
                    .check(matches(withContentDescription("$CALIFICACION de 5 estrellas")))
            }
        } finally {
            activityRule.scenario.onActivity {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }
}
