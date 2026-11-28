package co.edu.uniandes.misw4203.vinilos.accesibilidad

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import co.edu.uniandes.misw4203.vinilos.MainActivity
import co.edu.uniandes.misw4203.vinilos.R
import co.edu.uniandes.misw4203.vinilos.utils.AccessibilityTestUtils.revisarAccesibilidad
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.abrirDetalleDeAlbum
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.grantLocalNetworkAccess
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.waitFor
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accesibilidad de HU01 (catálogo), HU02 (detalle del álbum), HU03 (listado de artistas),
 * HU04 (detalle de un artista) y de los formularios de HU07, HU08 y HU09, revisada con el
 * motor del Accessibility Scanner (ATF). Backend real con los datos por defecto: no escribe
 * nada (los formularios solo se abren y se les pide guardar vacíos para ver los errores).
 */
@RunWith(AndroidJUnit4::class)
class AccesibilidadCatalogoE2ETest {

    @get:Rule(order = 0)
    val permissionRule = grantLocalNetworkAccess()

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private companion object {
        const val ALBUM = "Buscando América"
        const val MUSICO = "Rubén Blades Bellido de Luna"
    }

    private fun entrarComoVisitante() {
        onView(withId(R.id.visitorCard)).perform(click())
    }

    private fun entrarComoColeccionista() {
        onView(withId(R.id.collectorCard)).perform(click())
    }

    private fun esperarCatalogo() {
        waitFor { onView(withId(R.id.albumList)).check(matches(hasMinimumChildCount(1))) }
    }

    private fun abrirDetalleDelAlbum() {
        esperarCatalogo()
        abrirDetalleDeAlbum(ALBUM)
        waitFor { onView(withId(R.id.albumName)).check(matches(withText(ALBUM))) }
    }

    private fun abrirDetalleDelMusico() {
        waitFor { onView(withId(R.id.artistList)).check(matches(hasDescendant(withText(MUSICO)))) }
        onView(withId(R.id.artistList)).perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(MUSICO)),
                click()
            )
        )
        waitFor { onView(withId(R.id.artistName)).check(matches(withText(MUSICO))) }
    }

    // A11Y-HU01-01
    @Test
    fun a11y_hu01_01_catalogo_de_albumes_como_visitante() {
        entrarComoVisitante()
        esperarCatalogo()
        onView(isRoot()).perform(revisarAccesibilidad())
    }

    // A11Y-HU01-02 (con el botón «Agregar álbum» del coleccionista)
    @Test
    fun a11y_hu01_02_catalogo_de_albumes_como_coleccionista() {
        entrarComoColeccionista()
        esperarCatalogo()
        waitFor { onView(withId(R.id.addAlbumButton)).check(matches(isDisplayed())) }
        onView(isRoot()).perform(revisarAccesibilidad())
    }

    // A11Y-HU02-01
    @Test
    fun a11y_hu02_01_detalle_de_buscando_america_como_visitante() {
        entrarComoVisitante()
        abrirDetalleDelAlbum()
        onView(isRoot()).perform(revisarAccesibilidad())
    }

    // A11Y-HU02-02 (con los botones de agregar track, comentar y agregar a colección)
    @Test
    fun a11y_hu02_02_detalle_de_buscando_america_como_coleccionista() {
        entrarComoColeccionista()
        abrirDetalleDelAlbum()
        onView(withId(R.id.addTrackButton)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(isRoot()).perform(revisarAccesibilidad())
    }

    // A11Y-HU03-01
    @Test
    fun a11y_hu03_01_listado_de_artistas() {
        entrarComoVisitante()
        onView(withId(R.id.artistListFragment)).perform(click())
        waitFor { onView(withId(R.id.artistList)).check(matches(hasMinimumChildCount(2))) }
        onView(isRoot()).perform(revisarAccesibilidad())
    }

    // A11Y-HU04-01
    @Test
    fun a11y_hu04_01_detalle_de_ruben_blades_como_visitante() {
        entrarComoVisitante()
        onView(withId(R.id.artistListFragment)).perform(click())
        abrirDetalleDelMusico()
        onView(isRoot()).perform(revisarAccesibilidad())
    }

    // A11Y-HU04-02 (con el botón «Agregar a favoritos» del coleccionista)
    @Test
    fun a11y_hu04_02_detalle_de_ruben_blades_como_coleccionista() {
        entrarComoColeccionista()
        onView(withId(R.id.artistListFragment)).perform(click())
        abrirDetalleDelMusico()
        waitFor { onView(withId(R.id.addFavoriteButton)).perform(scrollTo()).check(matches(isDisplayed())) }
        onView(isRoot()).perform(revisarAccesibilidad())
    }

    // ---- HU07: Agregar álbum ----

    private fun abrirFormularioDeAlbum() {
        entrarComoColeccionista()
        waitFor { onView(withId(R.id.addAlbumButton)).check(matches(isDisplayed())) }
        onView(withId(R.id.addAlbumButton)).perform(click())
        onView(withId(R.id.topBarTitle)).check(matches(withText("Agregar álbum")))
    }

    // A11Y-HU07-01
    @Test
    fun a11y_hu07_01_formulario_de_album_vacio() {
        abrirFormularioDeAlbum()
        onView(isRoot()).perform(revisarAccesibilidad())
    }

    // A11Y-HU07-02
    @Test
    fun a11y_hu07_02_formulario_de_album_con_errores() {
        abrirFormularioDeAlbum()
        closeSoftKeyboard()
        onView(withId(R.id.saveButton)).perform(scrollTo(), click())
        waitFor {
            onView(withId(R.id.nameLayout)).check(
                matches(co.edu.uniandes.misw4203.vinilos.utils.TestUtils.conError("El nombre es obligatorio."))
            )
        }
        onView(isRoot()).perform(revisarAccesibilidad())
    }

    // ---- HU08: Agregar track ----

    private fun abrirFormularioDeTrack() {
        entrarComoColeccionista()
        abrirDetalleDelAlbum()
        onView(withId(R.id.addTrackButton)).perform(scrollTo(), click())
        onView(withId(R.id.topBarTitle)).check(matches(withText("Agregar track")))
    }

    // A11Y-HU08-01
    @Test
    fun a11y_hu08_01_formulario_de_track_vacio() {
        abrirFormularioDeTrack()
        onView(isRoot()).perform(revisarAccesibilidad())
    }

    // A11Y-HU08-02
    @Test
    fun a11y_hu08_02_formulario_de_track_con_errores() {
        abrirFormularioDeTrack()
        closeSoftKeyboard()
        onView(withId(R.id.saveTrackButton)).perform(scrollTo(), click())
        waitFor { onView(withId(R.id.errorBanner)).check(matches(isDisplayed())) }
        onView(isRoot()).perform(revisarAccesibilidad())
    }

    // ---- HU09: Comentar un álbum ----

    private fun abrirFormularioDeComentario() {
        entrarComoColeccionista()
        abrirDetalleDelAlbum()
        onView(withId(R.id.addCommentButton)).perform(scrollTo(), click())
        waitFor { onView(withId(R.id.collectorInput)).check(matches(isDisplayed())) }
    }

    // A11Y-HU09-01
    @Test
    fun a11y_hu09_01_formulario_de_comentario_vacio() {
        abrirFormularioDeComentario()
        onView(isRoot()).perform(revisarAccesibilidad())
    }

    // A11Y-HU09-02
    @Test
    fun a11y_hu09_02_formulario_de_comentario_con_errores() {
        abrirFormularioDeComentario()
        closeSoftKeyboard()
        onView(withId(R.id.saveButton)).perform(scrollTo(), click())
        waitFor { onView(withId(R.id.errorBanner)).check(matches(isDisplayed())) }
        onView(isRoot()).perform(revisarAccesibilidad())
    }
}
