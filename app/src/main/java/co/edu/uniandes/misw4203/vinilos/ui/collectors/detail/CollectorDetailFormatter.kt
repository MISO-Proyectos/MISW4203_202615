package co.edu.uniandes.misw4203.vinilos.ui.collectors.detail

/** Formatos de texto del detalle de un coleccionista (HU06), sin depender de Android para poder probarlos. */
object CollectorDetailFormatter {

    /** "35" para el precio de un álbum de la colección (el diseño muestra «Precio: 35», sin signo). */
    fun formatPrice(price: Int): String = price.toString()

    /** El backend manda el estado como "Active" o "Inactive". Cualquier otro valor se trata como inactivo. */
    fun isActive(status: String): Boolean = status.equals("Active", ignoreCase = true)
}
