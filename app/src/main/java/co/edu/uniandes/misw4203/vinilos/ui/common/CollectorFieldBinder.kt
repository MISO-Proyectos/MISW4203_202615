package co.edu.uniandes.misw4203.vinilos.ui.common

import co.edu.uniandes.misw4203.vinilos.R
import co.edu.uniandes.misw4203.vinilos.data.model.Collector
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout

/**
 * Pinta y escucha el campo "Coleccionista" de `include_collector_field.xml`, que usan
 * los formularios de HU09, HU10 y HU11 (la app no tiene inicio de sesión, así que el
 * usuario elige quién hace el registro).
 *
 * No hay clase base para los ViewModel: cada uno recibe `CollectorRepository`, llama
 * `getCollectors()` (que usa la caché que ya existe) y le pasa el resultado a [render].
 *
 * @param onSelected recibe el id del coleccionista elegido.
 * @param onRetry se llama cuando el usuario toca el ícono de reintentar (solo si la carga falló).
 */
class CollectorFieldBinder(
    private val layout: TextInputLayout,
    private val input: MaterialAutoCompleteTextView,
    private val onSelected: (Int) -> Unit,
    private val onRetry: () -> Unit
) {

    private var collectors: List<Collector> = emptyList()
    private var loadFailed = false

    init {
        // El texto lo pone render() desde el estado; si la vista lo restaurara sola,
        // la lista quedaría filtrada y solo mostraría la opción ya elegida
        input.isSaveEnabled = false
        input.setOnItemClickListener { _, _, position, _ ->
            collectors.getOrNull(position)?.let { onSelected(it.id) }
        }
    }

    /**
     * @param collectors lista a mostrar (vacía mientras carga o si falló).
     * @param selectedId coleccionista elegido, o null.
     * @param loadFailed si es true muestra el error de carga con el ícono de reintentar.
     * @param error error del campo (obligatorio, duplicado...). Se ignora si [loadFailed].
     */
    fun render(
        collectors: List<Collector>,
        selectedId: Int?,
        loadFailed: Boolean,
        error: CharSequence?
    ) {
        if (collectors.map { it.id to it.name } != this.collectors.map { it.id to it.name }) {
            this.collectors = collectors
            // setSimpleItems usa un adaptador que no filtra por lo escrito
            input.setSimpleItems(collectors.map { it.name }.toTypedArray())
        }

        val selectedName = collectors.firstOrNull { it.id == selectedId }?.name.orEmpty()
        if (input.text.toString() != selectedName) input.setText(selectedName, false)

        setRetryMode(loadFailed)
        val message = if (loadFailed) layout.context.getString(R.string.collector_field_error_load) else error
        if (layout.error?.toString() != message?.toString()) layout.error = message
    }

    private fun setRetryMode(failed: Boolean) {
        if (failed == loadFailed) return
        loadFailed = failed
        if (failed) {
            layout.endIconMode = TextInputLayout.END_ICON_CUSTOM
            layout.setEndIconDrawable(R.drawable.ic_refresh)
            layout.endIconContentDescription = layout.context.getString(R.string.collector_field_retry)
            layout.setEndIconOnClickListener { onRetry() }
        } else {
            // Sin listener propio, el ícono vuelve a abrir y cerrar la lista
            layout.setEndIconOnClickListener(null)
            layout.endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU
            layout.endIconContentDescription = layout.context.getString(R.string.collector_field_dropdown)
        }
    }
}
