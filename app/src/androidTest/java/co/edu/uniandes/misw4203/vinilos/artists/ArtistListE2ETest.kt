package co.edu.uniandes.misw4203.vinilos.artists

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeDown
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
 * HU03 - Consultar el listado de artistas (E2E con el backend real).
 * Precondición: backend BackVynils recién levantado con su base vacía; al arrancar
 * carga por defecto un músico (Rubén Blades, id 100) y una banda (Queen, id 101).
 */
@RunWith(AndroidJUnit4::class)
class ArtistListE2ETest {

    @get:Rule(order = 0)
    val permissionRule = grantLocalNetworkAccess()

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    // La app abre en la pantalla de rol: se entra como Visitante y se abre la sección Artistas.
    @Before
    fun entrar_como_visitante_y_abrir_artistas() {
        onView(withId(R.id.visitorCard)).perform(click())
        onView(withId(R.id.artistListFragment)).perform(click())
    }

    private companion object {
        const val MUSICO = "Rubén Blades Bellido de Luna"
        const val BANDA = "Queen"
        const val TIPO_MUSICO = "Músico"
        const val TIPO_BANDA = "Banda"
        const val CONTADOR = "2 artistas"
    }

    private fun laListaMuestra(texto: String) = waitFor {
        onView(withId(R.id.artistList)).check(matches(hasDescendant(withText(texto))))
    }

    // CP-HU03-01
    @Test
    fun listado_muestra_al_menos_un_artista() {
        waitFor { onView(withId(R.id.artistList)).check(matches(isDisplayed())) }
        waitFor { onView(withId(R.id.artistList)).check(matches(hasMinimumChildCount(1))) }
    }

    // CP-HU03-02
    @Test
    fun listado_muestra_musicos_y_bandas_con_su_tipo() {
        laListaMuestra(MUSICO)
        laListaMuestra(BANDA)
        laListaMuestra(TIPO_MUSICO)
        laListaMuestra(TIPO_BANDA)
    }

    // CP-HU03-03 (el detalle llega con la HU04: por ahora se muestra un aviso)
    @Test
    fun tocar_un_artista_muestra_el_aviso_del_detalle() {
        laListaMuestra(BANDA)
        onView(withId(R.id.artistList)).perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(BANDA)),
                click()
            )
        )
        waitFor {
            onView(withText(R.string.artist_detail_coming_soon)).check(matches(isDisplayed()))
        }
    }

    // CP-HU03-04
    @Test
    fun listado_muestra_el_titulo_de_la_pantalla() {
        onView(withId(R.id.topBarTitle)).check(matches(withText(R.string.nav_artists)))
    }

    // CP-HU03-05
    @Test
    fun listado_muestra_el_contador_de_artistas() {
        laListaMuestra(CONTADOR)
    }

    // CP-HU03-06
    @Test
    fun deslizar_hacia_abajo_recarga_y_sigue_mostrando_la_lista() {
        laListaMuestra(MUSICO)

        onView(withId(R.id.swipeRefresh)).perform(swipeDown())

        laListaMuestra(MUSICO)
        laListaMuestra(BANDA)
    }
}
