package co.edu.uniandes.misw4203.vinilos.collectors

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import co.edu.uniandes.misw4203.vinilos.MainActivity
import co.edu.uniandes.misw4203.vinilos.R
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.clicDirecto
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.grantLocalNetworkAccess
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.waitFor
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * HU06 - Consultar el detalle de un coleccionista (E2E con el backend real).
 * Precondición: backend BackVynils recién levantado con su base vacía; al arrancar
 * carga por defecto a Manolo Bellon (id 100), que tiene el álbum Buscando América,
 * a Rubén Blades como favorito y un comentario, y a Jaime Monsalve (id 101).
 */
@RunWith(AndroidJUnit4::class)
class CollectorDetailE2ETest {

    @get:Rule(order = 0)
    val permissionRule = grantLocalNetworkAccess()

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    // Entra como Visitante, abre Coleccionistas y toca a Manolo Bellon.
    @Before
    fun entrar_como_visitante_y_abrir_el_detalle_de_manolo() {
        onView(withId(R.id.visitorCard)).perform(click())
        onView(withId(R.id.collectorListFragment)).perform(click())
        waitFor { onView(withId(R.id.collectorList)).check(matches(hasDescendant(withText(COLECCIONISTA)))) }
        onView(withId(R.id.collectorList)).perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(COLECCIONISTA)),
                click()
            )
        )
        waitFor { onView(withId(R.id.collectorName)).check(matches(withText(COLECCIONISTA))) }
    }

    private companion object {
        const val COLECCIONISTA = "Manolo Bellon"
        const val TELEFONO = "3502457896"
        const val CORREO = "manollo@caracol.com.co"
        const val ALBUM = "Buscando América"
        const val FAVORITO = "Rubén Blades Bellido de Luna"
        const val COMENTARIO = "The most relevant album of Ruben Blades"
    }

    // CP-HU06-01
    @Test
    fun el_detalle_muestra_nombre_telefono_y_correo() {
        onView(withId(R.id.collectorName)).check(matches(withText(COLECCIONISTA)))
        onView(withId(R.id.collectorTelephone)).check(matches(withText(TELEFONO)))
        onView(withId(R.id.collectorEmail)).check(matches(withText(CORREO)))
        onView(withId(R.id.topBarTitle)).check(matches(withText(R.string.collector_detail_title)))
    }

    // CP-HU06-02
    @Test
    fun el_detalle_muestra_el_album_de_la_coleccion() {
        onView(withText(ALBUM)).perform(scrollTo())
        onView(withText(ALBUM)).check(matches(isDisplayed()))
    }

    // CP-HU06-03
    @Test
    fun el_detalle_muestra_el_artista_favorito() {
        onView(withText(FAVORITO)).perform(scrollTo())
        onView(withText(FAVORITO)).check(matches(isDisplayed()))
    }

    // CP-HU06-04
    @Test
    fun el_detalle_muestra_el_comentario() {
        onView(withText(COMENTARIO)).perform(scrollTo())
        onView(withText(COMENTARIO)).check(matches(isDisplayed()))
    }

    // CP-HU06-05
    @Test
    fun la_flecha_de_volver_regresa_al_listado_de_coleccionistas() {
        onView(withContentDescription(R.string.navigate_up)).perform(clicDirecto())

        waitFor { onView(withId(R.id.collectorList)).check(matches(isDisplayed())) }
        onView(withId(R.id.topBarTitle)).check(matches(withText(R.string.nav_collectors)))
    }

    // Extra: tocar un álbum de la colección abre el detalle del álbum (HU02)
    @Test
    fun tocar_un_album_de_la_coleccion_abre_el_detalle_del_album() {
        onView(withText(ALBUM)).perform(scrollTo(), click())

        waitFor { onView(withId(R.id.albumName)).check(matches(withText(ALBUM))) }
        onView(withId(R.id.topBarTitle)).check(matches(withText(R.string.album_detail_title)))
    }
}
