package co.edu.uniandes.misw4203.vinilos.albums

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import co.edu.uniandes.misw4203.vinilos.MainActivity
import co.edu.uniandes.misw4203.vinilos.R
import co.edu.uniandes.misw4203.vinilos.ui.albums.create.CreateAlbumFormatter
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.clicDirecto
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.conError
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.grantLocalNetworkAccess
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.sinError
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.waitFor
import com.google.android.material.datepicker.MaterialDatePicker
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * HU07 - Crear un álbum (E2E con el backend real).
 * Precondición: backend BackVynils levantado. La prueba que guarda un álbum usa un
 * nombre único, así que no depende de los álbumes que ya existan; al terminar ese álbum
 * queda en el backend (al final del catálogo, sin afectar las pruebas de HU01 y HU02).
 */
@RunWith(AndroidJUnit4::class)
class CreateAlbumE2ETest {

    @get:Rule(order = 0)
    val permissionRule = grantLocalNetworkAccess()

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private companion object {
        const val TITULO = "Agregar álbum"
        // Cada error de "obligatorio" nombra su campo (accesibilidad, A11Y-07)
        val OBLIGATORIOS = mapOf(
            R.id.nameLayout to "El nombre es obligatorio.",
            R.id.coverLayout to "La URL de la portada es obligatoria.",
            R.id.releaseDateLayout to "La fecha de lanzamiento es obligatoria.",
            R.id.genreLayout to "El género es obligatorio.",
            R.id.recordLabelLayout to "El sello discográfico es obligatorio.",
            R.id.descriptionLayout to "La descripción es obligatoria."
        )
        const val URL_INVALIDA = "Ingresa un enlace válido que empiece por http:// o https://."
        const val PORTADA = "https://i.pinimg.com/564x/aa/5f/ed/aa5fed7fac61cc8f41d1e79db917a7cd.jpg"
        const val DESCRIPCION = "Álbum creado por la prueba E2E de la HU07."
    }

    private fun entrarComoColeccionista() {
        onView(withId(R.id.collectorCard)).perform(click())
    }

    private fun abrirFormulario() {
        entrarComoColeccionista()
        waitFor { onView(withId(R.id.addAlbumButton)).check(matches(isDisplayed())) }
        onView(withId(R.id.addAlbumButton)).perform(click())
        onView(withId(R.id.topBarTitle)).check(matches(withText(TITULO)))
    }

    /** Abre la lista desplegable del campo y toca la opción con ese texto. */
    private fun elegirOpcion(campo: Int, opcion: String) {
        onView(withId(campo)).perform(scrollTo(), click())
        onView(withText(opcion)).inRoot(isPlatformPopup()).perform(click())
    }

    /** Abre el calendario y confirma la fecha que trae marcada (hoy). */
    private fun elegirFechaDeHoy() {
        onView(withId(R.id.releaseDateInput)).perform(scrollTo(), click())
        onView(withId(com.google.android.material.R.id.confirm_button)).perform(click())
    }

    private fun tocarGuardar() {
        closeSoftKeyboard()
        onView(withId(R.id.saveButton)).perform(scrollTo(), click())
    }

    // CP-HU07-01
    @Test
    fun visitante_no_ve_el_boton_agregar_album() {
        onView(withId(R.id.visitorCard)).perform(click())
        waitFor { onView(withId(R.id.albumList)).check(matches(isDisplayed())) }

        onView(withId(R.id.addAlbumButton))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    // CP-HU07-02
    @Test
    fun coleccionista_abre_el_formulario_con_todos_los_campos() {
        abrirFormulario()

        onView(withId(R.id.nameInput)).check(matches(isDisplayed()))
        onView(withId(R.id.coverInput)).check(matches(isDisplayed()))
        onView(withId(R.id.releaseDateInput)).check(matches(isDisplayed()))
        onView(withId(R.id.genreInput)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withId(R.id.recordLabelInput)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withId(R.id.descriptionInput)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withId(R.id.saveButton)).perform(scrollTo()).check(matches(isDisplayed()))
        // La barra superior muestra la "X" para cerrar y no la barra inferior
        onView(withContentDescription(R.string.close)).check(matches(isDisplayed()))
        onView(withId(R.id.bottomNavigation))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    // CP-HU07-03
    @Test
    fun guardar_sin_datos_marca_todos_los_campos_obligatorios() {
        abrirFormulario()

        tocarGuardar()

        OBLIGATORIOS.forEach { (campo, mensaje) ->
            onView(withId(campo)).check(matches(conError(mensaje)))
        }
        // Sigue en el formulario
        onView(withId(R.id.topBarTitle)).check(matches(withText(TITULO)))
    }

    // CP-HU07-04
    @Test
    fun una_portada_que_no_es_enlace_muestra_error() {
        abrirFormulario()

        onView(withId(R.id.coverInput)).perform(replaceText("portada.jpg"))
        tocarGuardar()

        onView(withId(R.id.coverLayout)).perform(scrollTo()).check(matches(conError(URL_INVALIDA)))
    }

    // CP-HU07-05
    @Test
    fun corregir_un_campo_quita_su_error() {
        abrirFormulario()
        tocarGuardar()
        onView(withId(R.id.nameLayout)).perform(scrollTo())
            .check(matches(conError(OBLIGATORIOS.getValue(R.id.nameLayout))))

        onView(withId(R.id.nameInput)).perform(replaceText("Un nombre"))
        elegirOpcion(R.id.genreInput, "Clásica")

        onView(withId(R.id.nameLayout)).check(matches(sinError()))
        onView(withId(R.id.genreLayout)).check(matches(sinError()))
        onView(withId(R.id.genreInput)).check(matches(withText("Clásica")))
        onView(withId(R.id.recordLabelLayout))
            .check(matches(conError(OBLIGATORIOS.getValue(R.id.recordLabelLayout))))
    }

    // CP-HU07-06
    @Test
    fun la_fecha_se_elige_en_el_calendario() {
        abrirFormulario()

        elegirFechaDeHoy()

        // El calendario abre en el día de hoy (UTC); el campo lo muestra como DD/MM/AAAA
        val hoy = CreateAlbumFormatter.formatDate(
            CreateAlbumFormatter.fromUtcMillis(MaterialDatePicker.todayInUtcMilliseconds())
        )
        onView(withId(R.id.releaseDateInput)).check(matches(withText(hoy)))
    }

    // CP-HU07-07
    @Test
    fun cancelar_vuelve_al_catalogo() {
        abrirFormulario()

        onView(withId(R.id.cancelButton)).perform(scrollTo(), click())

        onView(withId(R.id.topBarTitle)).check(matches(withText(R.string.nav_albums)))
        onView(withId(R.id.addAlbumButton)).check(matches(isDisplayed()))
    }

    // CP-HU07-08
    @Test
    fun la_x_de_la_barra_superior_cierra_el_formulario() {
        abrirFormulario()

        onView(withContentDescription(R.string.close)).perform(clicDirecto())

        onView(withId(R.id.topBarTitle)).check(matches(withText(R.string.nav_albums)))
    }

    // CP-HU07-09
    @Test
    fun atras_del_sistema_cierra_el_formulario() {
        abrirFormulario()
        closeSoftKeyboard()

        pressBack()

        onView(withId(R.id.topBarTitle)).check(matches(withText(R.string.nav_albums)))
    }

    // CP-HU07-10
    @Test
    fun crear_un_album_valido_lo_muestra_en_el_catalogo() {
        val nombre = "Álbum E2E ${System.currentTimeMillis()}"
        abrirFormulario()

        onView(withId(R.id.nameInput)).perform(replaceText(nombre))
        onView(withId(R.id.coverInput)).perform(replaceText(PORTADA))
        closeSoftKeyboard()
        elegirFechaDeHoy()
        elegirOpcion(R.id.genreInput, "Salsa")
        elegirOpcion(R.id.recordLabelInput, "Fania Records")
        onView(withId(R.id.descriptionInput)).perform(scrollTo(), replaceText(DESCRIPCION))
        tocarGuardar()

        // Vuelve al catálogo con el aviso y el álbum nuevo en la lista
        waitFor { onView(withText("Álbum «$nombre» creado")).check(matches(isDisplayed())) }
        onView(withId(R.id.topBarTitle)).check(matches(withText(R.string.nav_albums)))
        waitFor {
            onView(withId(R.id.albumList)).perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(withText(nombre)))
            )
        }
        onView(withText(nombre)).check(matches(isDisplayed()))
    }
}
