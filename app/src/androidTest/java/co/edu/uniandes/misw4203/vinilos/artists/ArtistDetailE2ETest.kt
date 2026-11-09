package co.edu.uniandes.misw4203.vinilos.artists

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
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
 * HU04 - Consultar el detalle de un artista (E2E con el backend real).
 * Precondición: backend BackVynils recién levantado con su base vacía; al arrancar
 * carga por defecto a Rubén Blades (músico, id 100) y Queen (banda, id 101).
 */
@RunWith(AndroidJUnit4::class)
class ArtistDetailE2ETest {

    @get:Rule(order = 0)
    val permissionRule = grantLocalNetworkAccess()

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    // Entra como Visitante y navega a la sección Artistas.
    @Before
    fun entrar_como_visitante_y_abrir_artistas() {
        onView(withId(R.id.visitorCard)).perform(click())
        onView(withId(R.id.artistListFragment)).perform(click())
    }

    private companion object {
        const val MUSICO = "Rubén Blades Bellido de Luna"
        const val BANDA = "Queen"
        const val ALBUM_MUSICO = "Buscando América"
    }

    private fun abrirDetalleDeArtista(nombre: String) {
        waitFor { onView(withId(R.id.artistList)).check(matches(hasDescendant(withText(nombre)))) }
        onView(withId(R.id.artistList)).perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(nombre)),
                click()
            )
        )
    }

    // CP-HU04-01: Abrir detalle de un músico
    @Test
    fun el_detalle_de_un_musico_muestra_sus_datos() {
        abrirDetalleDeArtista(MUSICO)

        waitFor { onView(withId(R.id.artistName)).check(matches(withText(MUSICO))) }
        onView(withId(R.id.artistType)).check(matches(withText(R.string.artist_type_musician)))
        onView(withId(R.id.dateLabel)).check(matches(withText(R.string.artist_birth_date)))
        onView(withId(R.id.artistDescription)).perform(scrollTo())
        onView(withId(R.id.artistDescription)).check(matches(isDisplayed()))
    }

    // CP-HU04-02: Abrir detalle de una banda
    @Test
    fun el_detalle_de_una_banda_muestra_sus_datos() {
        abrirDetalleDeArtista(BANDA)

        waitFor { onView(withId(R.id.artistName)).check(matches(withText(BANDA))) }
        onView(withId(R.id.artistType)).check(matches(withText(R.string.artist_type_band)))
        onView(withId(R.id.dateLabel)).check(matches(withText(R.string.artist_creation_date)))
    }

    // CP-HU04-03: Botón volver regresa al listado
    @Test
    fun la_flecha_de_volver_regresa_al_listado_de_artistas() {
        abrirDetalleDeArtista(MUSICO)
        waitFor { onView(withId(R.id.artistName)).check(matches(withText(MUSICO))) }

        onView(withContentDescription(R.string.navigate_up)).perform(clicDirecto())

        waitFor { onView(withId(R.id.artistList)).check(matches(isDisplayed())) }
        onView(withId(R.id.topBarTitle)).check(matches(withText(R.string.nav_artists)))
    }

    // CP-HU04-04: Navegar de un álbum asociado al detalle del álbum
    @Test
    fun tocar_un_album_del_artista_abre_el_detalle_del_album() {
        abrirDetalleDeArtista(MUSICO)
        waitFor { onView(withId(R.id.artistName)).check(matches(withText(MUSICO))) }

        onView(withText(ALBUM_MUSICO)).perform(scrollTo(), click())

        waitFor { onView(withId(R.id.albumName)).check(matches(withText(ALBUM_MUSICO))) }
        onView(withId(R.id.topBarTitle)).check(matches(withText(R.string.album_detail_title)))
    }
}
