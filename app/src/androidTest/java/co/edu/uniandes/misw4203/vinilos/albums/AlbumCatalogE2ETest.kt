package co.edu.uniandes.misw4203.vinilos.albums

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import co.edu.uniandes.misw4203.vinilos.MainActivity
import co.edu.uniandes.misw4203.vinilos.R
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.grantLocalNetworkAccess
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.waitFor
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * HU01 - Consultar catálogo de álbumes (E2E con el backend real).
 * Precondición: backend BackVynils recién levantado con su base vacía; al arrancar
 * carga solos los 4 álbumes por defecto (ids 100 a 103) que usan estas pruebas.
 */
@RunWith(AndroidJUnit4::class)
class AlbumCatalogE2ETest {

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
        const val PRIMER_ALBUM = "Buscando América"
        const val GENERO_PRIMER_ALBUM = "Salsa"
        const val ANIO_PRIMER_ALBUM = "1984"
        const val ULTIMO_ALBUM = "A Day at the Races"
        // El contador ocupa la posición 0 del ConcatAdapter; los 4 álbumes van de 1 a 4.
        const val ULTIMA_POSICION = 4
    }

    // CP-HU01-01
    @Test
    fun catalogo_muestra_la_lista_de_albumes() {
        waitFor { onView(withId(R.id.albumList)).check(matches(isDisplayed())) }
        waitFor { onView(withId(R.id.albumList)).check(matches(hasMinimumChildCount(1))) }
    }

    // CP-HU01-02
    @Test
    fun catalogo_muestra_los_datos_del_primer_album() {
        waitFor {
            onView(withId(R.id.albumList))
                .check(matches(hasDescendant(withText(PRIMER_ALBUM))))
        }
        waitFor {
            onView(withId(R.id.albumList))
                .check(matches(hasDescendant(withText(GENERO_PRIMER_ALBUM))))
        }
        waitFor {
            onView(withId(R.id.albumList))
                .check(matches(hasDescendant(withText(ANIO_PRIMER_ALBUM))))
        }
    }

    // CP-HU01-03
    @Test
    fun catalogo_permite_llegar_al_ultimo_album() {
        waitFor { onView(withId(R.id.albumList)).check(matches(hasMinimumChildCount(1))) }

        onView(withId(R.id.albumList))
            .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(ULTIMA_POSICION))

        onView(withId(R.id.albumList))
            .check(matches(hasDescendant(withText(ULTIMO_ALBUM))))
    }

    // CP-HU01-04
    @Test
    fun barra_inferior_muestra_las_tres_secciones() {
        onView(withId(R.id.albumListFragment)).check(matches(isDisplayed()))
        onView(withId(R.id.artistListFragment)).check(matches(isDisplayed()))
        onView(withId(R.id.collectorListFragment)).check(matches(isDisplayed()))
    }

    // CP-HU01-05
    @Test
    fun se_puede_navegar_a_otra_seccion_y_volver_al_catalogo() {
        onView(withId(R.id.artistListFragment)).perform(click())
        onView(withId(R.id.placeholderMessage)).check(matches(withText(R.string.placeholder_next_sprint)))

        onView(withId(R.id.albumListFragment)).perform(click())
        waitFor { onView(withId(R.id.albumList)).check(matches(isDisplayed())) }
    }

    // CP-HU01-11 (título de la pantalla)
    @Test
    fun catalogo_muestra_el_titulo_de_la_pantalla() {
        onView(withId(R.id.topBarTitle)).check(matches(withText(R.string.nav_albums)))
    }
}
