package co.edu.uniandes.misw4203.vinilos.ui.collectors.detail

import java.text.NumberFormat

/** Formatos de texto del detalle de un coleccionista (HU06), sin depender de Android para poder probarlos. */
object CollectorDetailFormatter {

    /** "35" para el precio de un álbum de la colección (el diseño muestra «Precio: 35», sin signo). */
    fun formatPrice(price: Int): String = price.toString()

    /** Cantidad para los contadores de sección, con los dígitos del idioma del dispositivo. */
    fun formatCount(count: Int): String = NumberFormat.getIntegerInstance().format(count)

    /** El backend manda el estado como "Active" o "Inactive". Cualquier otro valor se trata como inactivo. */
    fun isActive(status: String): Boolean = status.equals("Active", ignoreCase = true)
}
