package co.edu.uniandes.misw4203.vinilos.utils

import android.view.View
import android.widget.TextView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import co.edu.uniandes.misw4203.vinilos.R
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityViewCheckResult
import com.google.android.apps.common.testing.accessibility.framework.checks.DuplicateSpeakableTextCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.TouchTargetSizeCheck
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.anyOf
import org.hamcrest.TypeSafeMatcher

/**
 * Revisión de accesibilidad con el mismo motor del Accessibility Scanner (Accessibility Test
 * Framework). Se aplica solo cuando la prueba la pide, sobre la vista raíz de ese momento.
 * No se usa AccessibilityChecks.enable(): queda activo para todo el proceso y haría fallar
 * a las demás pruebas E2E.
 */
object AccessibilityTestUtils {

    /** Una línea por resultado: tipo, chequeo, id del elemento y mensaje. */
    private fun describir(r: AccessibilityViewCheckResult): String {
        val vista = r.view
        val id = when {
            vista == null -> "(sin vista)"
            vista.id == View.NO_ID -> "(sin id)"
            else -> runCatching { vista.resources.getResourceEntryName(vista.id) }
                .getOrDefault("#${vista.id}")
        }
        return "${r.type} | ${(r.accessibilityHierarchyCheck?.simpleName ?: r.sourceCheckClass.simpleName)} | $id | ${r.message}"
    }

    /**
     * Falso positivo comprobado: ATF marca el botón de volver (topBarIcon) con un ancho de 36 dp,
     * pero la vista mide 48 x 48 dp (A11Y-02). Su marginStart de -12 dp la mete en el relleno de
     * la barra, y ATF toma como tamaño lo que el padre recorta al dibujar (clipToPadding); el
     * toque no se recorta. El Accessibility Scanner no lo marca. Solo se suprime este chequeo
     * para este id; cualquier otro hallazgo sigue haciendo fallar la prueba.
     */
    private fun falsoPositivoDeLaBarraSuperior(): Matcher<AccessibilityViewCheckResult> =
        object : TypeSafeMatcher<AccessibilityViewCheckResult>() {
            override fun describeTo(description: Description) {
                description.appendText("TouchTargetSizeCheck sobre topBarIcon")
            }

            override fun matchesSafely(r: AccessibilityViewCheckResult): Boolean {
                val vista = r.view ?: return false
                return r.accessibilityHierarchyCheck == TouchTargetSizeCheck::class.java &&
                    vista.id != View.NO_ID &&
                    vista.resources.getResourceEntryName(vista.id) == "topBarIcon"
            }
        }

    /**
     * Texto duplicado entre la etiqueta y su campo (Coleccionista y Precio de HU11). La etiqueta
     * y el campo son dos paradas de foco unidas por labelFor: es el mismo patrón de HU07 y HU08,
     * que el Accessibility Scanner acepta sin sugerencias. El contentDescription de la etiqueta
     * ("... obligatorio") es lo que evita que se lea "asterisco", y labelFor hace que el campo
     * se anuncie con su nombre (WCAG 1.3.1). Solo se suprime este chequeo para estos dos ids.
     */
    private fun duplicadoDeEtiquetaYCampo(): Matcher<AccessibilityViewCheckResult> =
        object : TypeSafeMatcher<AccessibilityViewCheckResult>() {
            override fun describeTo(description: Description) {
                description.appendText("DuplicateSpeakableTextCheck sobre collectorInput o priceInput")
            }

            override fun matchesSafely(r: AccessibilityViewCheckResult): Boolean {
                val vista = r.view ?: return false
                return r.accessibilityHierarchyCheck == DuplicateSpeakableTextCheck::class.java &&
                    vista.id != View.NO_ID &&
                    vista.resources.getResourceEntryName(vista.id) in
                    setOf("collectorInput", "priceInput")
            }
        }

    /**
     * Texto duplicado entre el título de la pantalla "Agregar a favoritos" y su botón de acción
     * (HU10). El prototipo usa la misma frase en los dos: el título es un encabezado y el botón
     * se anuncia como botón, así que el usuario los distingue por su rol. Solo se suprime este
     * chequeo para el botón saveButton de ese formulario (el que tiene el texto
     * add_favorite_save); el saveButton de los demás formularios sigue revisándose.
     */
    private fun duplicadoDeTituloYBotonDeFavoritos(): Matcher<AccessibilityViewCheckResult> =
        object : TypeSafeMatcher<AccessibilityViewCheckResult>() {
            override fun describeTo(description: Description) {
                description.appendText("DuplicateSpeakableTextCheck sobre el saveButton de Agregar a favoritos")
            }

            override fun matchesSafely(r: AccessibilityViewCheckResult): Boolean {
                val vista = r.view as? TextView ?: return false
                return r.accessibilityHierarchyCheck == DuplicateSpeakableTextCheck::class.java &&
                    vista.id != View.NO_ID &&
                    vista.resources.getResourceEntryName(vista.id) == "saveButton" &&
                    vista.text.toString() == vista.context.getString(R.string.add_favorite_save)
            }
        }

    /**
     * Corre los chequeos de ATF sobre toda la vista raíz y falla si hay errores o advertencias.
     * El mensaje de la falla lista cada hallazgo.
     */
    fun revisarAccesibilidad(): ViewAction = object : ViewAction {
        override fun getConstraints(): Matcher<View> = isRoot()
        override fun getDescription() = "revisar la accesibilidad con ATF"
        override fun perform(uiController: UiController, view: View) {
            val validator = AccessibilityValidator()
                .setRunChecksFromRootView(true)
                .setSuppressingResultMatcher(
                    anyOf(
                        falsoPositivoDeLaBarraSuperior(),
                        duplicadoDeEtiquetaYCampo(),
                        duplicadoDeTituloYBotonDeFavoritos()
                    )
                )
            val hallazgos = validator.checkAndReturnResults(view).filter {
                it.type == AccessibilityCheckResultType.ERROR ||
                    it.type == AccessibilityCheckResultType.WARNING
            }
            if (hallazgos.isNotEmpty()) {
                throw AssertionError(
                    "ATF encontró ${hallazgos.size} hallazgo(s):\n" +
                        hallazgos.joinToString("\n") { "  - ${describir(it)}" }
                )
            }
        }
    }
}
