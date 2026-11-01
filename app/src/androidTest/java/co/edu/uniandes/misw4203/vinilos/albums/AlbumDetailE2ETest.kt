package co.edu.uniandes.misw4203.vinilos.albums

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import co.edu.uniandes.misw4203.vinilos.MainActivity
import co.edu.uniandes.misw4203.vinilos.R
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.abrirDetalleDeAlbum
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

    // Faltan estos casos:
    // CP-HU02-03 volver al catálogo
    // CP-HU02-06 ver los tracks del álbum
    // CP-HU02-07 ver los comentarios del álbum
}
