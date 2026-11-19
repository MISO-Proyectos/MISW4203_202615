package co.edu.uniandes.misw4203.vinilos.collectors

import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isFocusable
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import co.edu.uniandes.misw4203.vinilos.MainActivity
import co.edu.uniandes.misw4203.vinilos.R
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.grantLocalNetworkAccess
import co.edu.uniandes.misw4203.vinilos.utils.TestUtils.waitFor
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.endsWith
import org.hamcrest.Matchers.startsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * HU06 - Accesibilidad del detalle de un coleccionista (A11Y-11 a A11Y-16).
 * Comprueba en la vista real lo que el Accessibility Scanner no revisa: encabezados, elementos
 * agrupados, descripción y acción de la fila de un álbum y regiones vivas de carga y error.
 * Precondición: backend BackVynils con sus datos por defecto (Manolo Bellon, id 100, con el álbum
 * Buscando América, a Rubén Blades como favorito y un comentario).
 */
@RunWith(AndroidJUnit4::class)
class CollectorDetailAccessibilityTest {

    @get:Rule(order = 0)
    val permissionRule = grantLocalNetworkAccess()

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun entrar_como_visitante_y_abrir_el_detalle_de_manolo() {
        onView(withId(R.id.visitorCard)).perform(click())
        waitFor(timeoutMs = ESPERA_LARGA_MS) {
            onView(withId(R.id.collectorListFragment)).perform(click())
            // El backend no ordena la lista y otras pruebas agregan coleccionistas: se desplaza hasta el elemento
            onView(withId(R.id.collectorList)).perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(withText(COLECCIONISTA)))
            )
        }
        onView(withId(R.id.collectorList)).perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(COLECCIONISTA)),
                click()
            )
        )
        waitFor(timeoutMs = ESPERA_LARGA_MS) {
            onView(withId(R.id.detailName)).check(matches(withText(COLECCIONISTA)))
        }
    }

    private companion object {
        const val ESPERA_LARGA_MS = 30_000L
        const val COLECCIONISTA = "Manolo Bellon"
        const val ALBUM = "Buscando América"
        const val COMENTARIO = "The most relevant album of Ruben Blades"
        const val ACCION_FILA = "ver el detalle del álbum"
    }

    /** Vista marcada como encabezado para el lector de pantalla (también en Android anterior a 9). */
    private fun esEncabezado(): Matcher<View> = object : BoundedMatcher<View, View>(View::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("vista marcada como encabezado de accesibilidad")
        }

        override fun matchesSafely(item: View) = ViewCompat.isAccessibilityHeading(item)
    }

    /** Vista con una región viva educada: el lector anuncia su contenido cuando aparece. */
    private fun esRegionViva(): Matcher<View> = object : BoundedMatcher<View, View>(View::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("región viva de accesibilidad (polite)")
        }

        override fun matchesSafely(item: View) =
            ViewCompat.getAccessibilityLiveRegion(item) == ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE
    }

    /** Vista cuya acción de toque se anuncia con la etiqueta dada ("Toca dos veces para …"). */
    private fun conAccionDeToque(etiqueta: String): Matcher<View> =
        object : BoundedMatcher<View, View>(View::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("vista con la acción de toque \"$etiqueta\"")
            }

            override fun matchesSafely(item: View) =
                item.createAccessibilityNodeInfo().actionList.any {
                    it.id == AccessibilityNodeInfo.ACTION_CLICK && it.label?.toString() == etiqueta
                }
        }

    // A11Y-11
    @Test
    fun el_nombre_y_los_titulos_de_seccion_son_encabezados() {
        onView(withId(R.id.detailName)).check(matches(esEncabezado()))
        onView(withId(R.id.performersTitle)).check(matches(esEncabezado()))
        onView(withId(R.id.albumsTitle)).check(matches(esEncabezado()))
        onView(withId(R.id.commentsTitle)).check(matches(esEncabezado()))
    }

    // A11Y-12
    @Test
    fun el_correo_y_el_telefono_se_leen_junto_con_su_etiqueta() {
        onView(withId(R.id.emailRow)).check(
            matches(
                allOf(
                    isFocusable(),
                    hasDescendant(withText(R.string.collector_email)),
                    hasDescendant(withId(R.id.detailEmail))
                )
            )
        )
        onView(withId(R.id.telephoneRow)).check(
            matches(
                allOf(
                    isFocusable(),
                    hasDescendant(withText(R.string.collector_telephone)),
                    hasDescendant(withId(R.id.detailTelephone))
                )
            )
        )
    }

    // A11Y-13
    @Test
    fun la_fila_de_album_tiene_descripcion_completa_y_accion() {
        val fila = allOf(withId(R.id.albumRow), hasDescendant(withText(ALBUM)))
        onView(fila).perform(scrollTo())

        onView(fila).check(
            matches(
                withContentDescription(
                    allOf(startsWith("$ALBUM, precio "), endsWith(", estado Activo"))
                )
            )
        )
        onView(fila).check(matches(conAccionDeToque(ACCION_FILA)))
    }

    // A11Y-16
    @Test
    fun el_comentario_se_lee_como_un_solo_elemento() {
        val fila = allOf(withId(R.id.commentRow), hasDescendant(withText(COMENTARIO)))
        onView(fila).perform(scrollTo())

        onView(fila).check(matches(allOf(isFocusable(), hasDescendant(withId(R.id.commentStars)))))
    }

    // A11Y-15
    @Test
    fun la_carga_y_el_error_se_anuncian_como_regiones_vivas() {
        onView(withId(R.id.loadingIndicator)).check(matches(esRegionViva()))
        onView(withId(R.id.errorContainer)).check(matches(esRegionViva()))
    }
}
