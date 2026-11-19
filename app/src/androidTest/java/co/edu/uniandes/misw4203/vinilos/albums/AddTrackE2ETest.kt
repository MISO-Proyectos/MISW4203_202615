package co.edu.uniandes.misw4203.vinilos.albums

import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import co.edu.uniandes.misw4203.vinilos.BuildConfig
import co.edu.uniandes.misw4203.vinilos.MainActivity
import co.edu.uniandes.misw4203.vinilos.R
import co.edu.uniandes.misw4203.vinilos.VinilosApplication
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.abrirDetalleDeAlbum
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.clicDirecto
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.conError
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.grantLocalNetworkAccess
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.sinError
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.waitFor
import org.hamcrest.Matchers.containsString
import org.json.JSONObject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL

/**
 * HU08 - Asociar tracks a un álbum (E2E con el backend real).
 * Precondición: backend BackVynils levantado. Para no cambiar los álbumes por defecto
 * que usan las pruebas de HU02 (que cuentan sus canciones), la primera prueba crea por
 * la API un álbum propio con nombre único y todas las canciones se agregan a ese álbum.
 */
@RunWith(AndroidJUnit4::class)
class AddTrackE2ETest {

    @get:Rule(order = 0)
    val permissionRule = grantLocalNetworkAccess()

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private companion object {
        const val TITULO = "Agregar track"
        const val OBLIGATORIO = "Este campo es obligatorio"
        const val FORMATO_DURACION = "Usa el formato MM:SS"

        /** Álbum de prueba; se crea una sola vez para toda la clase. */
        val ALBUM = "Álbum HU08 E2E ${System.currentTimeMillis()}"
        var albumCreado = false

        /**
         * Crea el álbum de prueba con POST /albums. Corre en el hilo de la prueba (no en el
         * principal) y después de conceder el permiso de red local.
         */
        fun crearAlbumDePrueba() {
            if (albumCreado) return
            val body = JSONObject()
                .put("name", ALBUM)
                .put("cover", "https://i.pinimg.com/564x/aa/5f/ed/aa5fed7fac61cc8f41d1e79db917a7cd.jpg")
                .put("releaseDate", "2020-01-01T00:00:00.000Z")
                .put("description", "Álbum creado por la prueba E2E de la HU08.")
                .put("genre", "Rock")
                .put("recordLabel", "EMI")
                .toString()
            val connection = URL(BuildConfig.BASE_URL + "albums").openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toByteArray()) }
                check(connection.responseCode in 200..299) {
                    "No se pudo crear el álbum de prueba (HTTP ${connection.responseCode})"
                }
            } finally {
                connection.disconnect()
            }
            // El catálogo pudo quedar en memoria por pruebas anteriores: se borra para que traiga el álbum nuevo
            ApplicationProvider.getApplicationContext<VinilosApplication>()
                .appContainer.albumRepository.clearCache()
            albumCreado = true
        }
    }

    @Before
    fun preparar() {
        crearAlbumDePrueba()
    }

    private fun abrirDetalle(rol: Int) {
        onView(withId(rol)).perform(click())
        // El álbum de prueba queda al final del catálogo: primero se desplaza hasta él
        waitFor {
            onView(withId(R.id.albumList)).perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(withText(ALBUM)))
            )
        }
        abrirDetalleDeAlbum(ALBUM)
        waitFor { onView(withId(R.id.albumName)).check(matches(withText(ALBUM))) }
    }

    private fun abrirFormulario() {
        abrirDetalle(R.id.collectorCard)
        onView(withId(R.id.addTrackButton)).perform(scrollTo(), click())
        onView(withId(R.id.topBarTitle)).check(matches(withText(TITULO)))
    }

    private fun tocarGuardar() {
        closeSoftKeyboard()
        onView(withId(R.id.saveTrackButton)).perform(scrollTo(), click())
    }

    private fun volvioAlDetalle() {
        waitFor { onView(withId(R.id.topBarTitle)).check(matches(withText(R.string.album_detail_title))) }
        onView(withId(R.id.albumName)).check(matches(withText(ALBUM)))
    }

    // CP-HU08-01
    @Test
    fun visitante_no_ve_el_boton_agregar_track() {
        abrirDetalle(R.id.visitorCard)

        onView(withId(R.id.addTrackButton))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    // CP-HU08-02
    @Test
    fun coleccionista_abre_el_formulario_con_el_album_y_los_campos() {
        abrirFormulario()

        waitFor { onView(withId(R.id.albumSummary)).check(matches(isDisplayed())) }
        onView(withId(R.id.albumSummary)).check(matches(hasDescendant(withText(ALBUM))))
        onView(withId(R.id.albumInfo)).check(matches(withText(containsString("track"))))
        onView(withId(R.id.trackNameInput)).check(matches(isDisplayed()))
        onView(withId(R.id.trackDurationInput)).check(matches(isDisplayed()))
        onView(withId(R.id.saveTrackButton)).check(matches(isDisplayed()))
        onView(withId(R.id.errorBanner)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        // La barra superior muestra la "X" para cerrar
        onView(withContentDescription(R.string.close)).check(matches(isDisplayed()))
    }

    // CP-HU08-03
    @Test
    fun guardar_sin_datos_muestra_el_aviso_y_los_errores() {
        abrirFormulario()

        tocarGuardar()

        onView(withId(R.id.errorBanner)).check(matches(isDisplayed()))
        onView(withId(R.id.trackNameLayout)).check(matches(conError(OBLIGATORIO)))
        onView(withId(R.id.trackDurationLayout)).check(matches(conError(FORMATO_DURACION)))
        onView(withId(R.id.topBarTitle)).check(matches(withText(TITULO)))
    }

    // CP-HU08-04
    @Test
    fun una_duracion_sin_formato_MM_SS_muestra_error() {
        abrirFormulario()

        onView(withId(R.id.trackNameInput)).perform(replaceText("Un track"))
        onView(withId(R.id.trackDurationInput)).perform(replaceText("5:75"))
        tocarGuardar()

        onView(withId(R.id.trackNameLayout)).check(matches(sinError()))
        onView(withId(R.id.trackDurationLayout)).check(matches(conError(FORMATO_DURACION)))
    }

    // CP-HU08-05
    @Test
    fun corregir_los_campos_quita_los_errores_y_el_aviso() {
        abrirFormulario()
        tocarGuardar()
        onView(withId(R.id.errorBanner)).check(matches(isDisplayed()))

        onView(withId(R.id.trackNameInput)).perform(replaceText("Un track"))
        onView(withId(R.id.trackDurationInput)).perform(replaceText("04:20"))

        onView(withId(R.id.trackNameLayout)).check(matches(sinError()))
        onView(withId(R.id.trackDurationLayout)).check(matches(sinError()))
        onView(withId(R.id.errorBanner)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    // CP-HU08-06
    @Test
    fun cancelar_vuelve_al_detalle_del_album() {
        abrirFormulario()

        closeSoftKeyboard()
        onView(withId(R.id.cancelButton)).perform(scrollTo(), click())

        volvioAlDetalle()
    }

    // CP-HU08-07
    @Test
    fun la_x_de_la_barra_superior_cierra_el_formulario() {
        abrirFormulario()

        onView(withContentDescription(R.string.close)).perform(clicDirecto())

        volvioAlDetalle()
    }

    // CP-HU08-08
    @Test
    fun atras_del_sistema_cierra_el_formulario() {
        abrirFormulario()
        closeSoftKeyboard()

        pressBack()

        volvioAlDetalle()
    }

    // CP-HU08-09
    @Test
    fun agregar_un_track_valido_lo_muestra_en_el_detalle() {
        val track = "Track E2E ${System.currentTimeMillis()}"
        abrirFormulario()

        onView(withId(R.id.trackNameInput)).perform(replaceText(track))
        // Se escribe sin el cero inicial: el detalle la muestra como 07:41
        onView(withId(R.id.trackDurationInput)).perform(replaceText("7:41"))
        tocarGuardar()

        // Vuelve al detalle con el aviso y la canción nueva en la lista
        waitFor { onView(withText("Track «$track» agregado")).check(matches(isDisplayed())) }
        volvioAlDetalle()
        waitFor { onView(withText(track)).perform(scrollTo()).check(matches(isDisplayed())) }
        onView(withText("07:41")).check(matches(isDisplayed()))
    }
}
