package co.edu.uniandes.misw4203.vinilos.albums

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import co.edu.uniandes.misw4203.vinilos.MainActivity
import co.edu.uniandes.misw4203.vinilos.R
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.abrirDetalleDeAlbum
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.clicDirecto
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.grantLocalNetworkAccess
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.waitFor
import org.hamcrest.Matchers.containsString
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * HU02 - Consultar el detalle de un álbum (E2E con el backend real).
 * Precondición: backend BackVynils recién levantado con su base vacía; al arrancar
 * carga solo los 4 álbumes por defecto (ids 100 a 103) que usan estas pruebas.
 * Buscando América (id 100) tiene 2 tracks y 1 comentario; Poeta del pueblo (id 101)
 * y A Day at the Races (id 103) no tienen tracks ni comentarios.
 */
@RunWith(AndroidJUnit4::class)
class AlbumDetailE2ETest {

    @get:Rule(order = 0)
    val permissionRule = grantLocalNetworkAccess()

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    // La app abre en la pantalla de rol: antes de cada prueba se entra como Visitante.
    @Before
    fun entrar_como_visitante() {
        onView(withId(R.id.visitorCard)).perform(click())
    }

    private companion object {
        const val ALBUM = "Buscando América"
        const val INTERPRETE = "Rubén Blades Bellido de Luna"
        const val FECHA = "1 ago 1984"
        const val GENERO = "Salsa"
        const val SELLO = "Elektra"
        const val INICIO_DESCRIPCION = "Buscando América es el primer álbum"
    }

    // CP-HU02-01
    @Test
    fun el_detalle_se_abre_desde_el_catalogo() {
        abrirDetalleDeAlbum(ALBUM)

        waitFor { onView(withId(R.id.albumName)).check(matches(withText(ALBUM))) }
        onView(withId(R.id.topBarTitle)).check(matches(withText(R.string.album_detail_title)))
    }

    // CP-HU02-02
    @Test
    fun el_detalle_muestra_los_datos_del_album() {
        abrirDetalleDeAlbum(ALBUM)

        waitFor { onView(withId(R.id.albumName)).check(matches(withText(ALBUM))) }
        onView(withId(R.id.albumPerformers)).check(matches(withText(INTERPRETE)))
        onView(withId(R.id.releaseDateValue)).check(matches(withText(FECHA)))
        onView(withId(R.id.genreValue)).check(matches(withText(GENERO)))
        onView(withId(R.id.recordLabelValue)).check(matches(withText(SELLO)))
        // La descripción queda abajo en pantallas pequeñas: se desplaza hasta verla
        onView(withId(R.id.albumDescription)).perform(scrollTo())
        onView(withId(R.id.albumDescription))
            .check(matches(withText(containsString(INICIO_DESCRIPCION))))
        onView(withId(R.id.albumDescription)).check(matches(isDisplayed()))
    }

    // CP-HU02-03 (con el botón Atrás del sistema)
    @Test
    fun el_boton_atras_regresa_al_catalogo() {
        abrirDetalleDeAlbum(ALBUM)
        waitFor { onView(withId(R.id.albumName)).check(matches(withText(ALBUM))) }

        pressBack()

        waitFor { onView(withId(R.id.albumList)).check(matches(isDisplayed())) }
        onView(withId(R.id.topBarTitle)).check(matches(withText(R.string.nav_albums)))
    }

    // CP-HU02-03 (con la flecha de la barra superior)
    @Test
    fun la_flecha_de_la_barra_regresa_al_catalogo() {
        abrirDetalleDeAlbum(ALBUM)
        waitFor { onView(withId(R.id.albumName)).check(matches(withText(ALBUM))) }

        onView(withContentDescription(R.string.navigate_up)).perform(clicDirecto())

        waitFor { onView(withId(R.id.albumList)).check(matches(isDisplayed())) }
        onView(withId(R.id.topBarTitle)).check(matches(withText(R.string.nav_albums)))
    }

    // CP-HU02-06
    @Test
    fun el_detalle_muestra_los_tracks_del_album() {
        abrirDetalleDeAlbum(ALBUM)
        waitFor { onView(withId(R.id.albumName)).check(matches(withText(ALBUM))) }

        onView(withId(R.id.tracksCount)).perform(scrollTo())
        onView(withId(R.id.tracksCount)).check(matches(withText("2 canciones")))
        onView(withText("Decisiones")).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText("05:05")).check(matches(isDisplayed()))
        onView(withText("Desapariciones")).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText("06:29")).check(matches(isDisplayed()))
    }

    // CP-HU02-07
    @Test
    fun el_detalle_muestra_los_comentarios_del_album() {
        abrirDetalleDeAlbum(ALBUM)
        waitFor { onView(withId(R.id.albumName)).check(matches(withText(ALBUM))) }

        onView(withId(R.id.commentsCount)).perform(scrollTo())
        onView(withId(R.id.commentsCount)).check(matches(withText("1")))
        onView(withText("The most relevant album of Ruben Blades"))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    // CP-HU02-04 (automático)
    @Test
    fun el_detalle_de_un_album_sin_tracks_muestra_un_mensaje() {
        abrirDetalleDeAlbum("Poeta del pueblo")
        waitFor { onView(withId(R.id.albumName)).check(matches(withText("Poeta del pueblo"))) }

        onView(withId(R.id.tracksCount)).perform(scrollTo())
        onView(withId(R.id.tracksCount)).check(matches(withText("0 canciones")))
        onView(withId(R.id.tracksEmpty)).perform(scrollTo())
        onView(withId(R.id.tracksEmpty)).check(matches(withText(R.string.album_no_tracks)))
    }
}
