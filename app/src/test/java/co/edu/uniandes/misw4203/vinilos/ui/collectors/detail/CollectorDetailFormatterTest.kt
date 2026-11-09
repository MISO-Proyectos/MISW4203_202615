package co.edu.uniandes.misw4203.vinilos.ui.collectors.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectorDetailFormatterTest {

    @Test
    fun `el precio se muestra como numero`() {
        assertEquals("35", CollectorDetailFormatter.formatPrice(35))
        assertEquals("0", CollectorDetailFormatter.formatPrice(0))
    }

    @Test
    fun `Active es activo sin importar mayusculas`() {
        assertTrue(CollectorDetailFormatter.isActive("Active"))
        assertTrue(CollectorDetailFormatter.isActive("active"))
    }

    @Test
    fun `cualquier otro estado es inactivo`() {
        assertFalse(CollectorDetailFormatter.isActive("Inactive"))
        assertFalse(CollectorDetailFormatter.isActive(""))
    }
}
