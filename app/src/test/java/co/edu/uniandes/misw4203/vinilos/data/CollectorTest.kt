package co.edu.uniandes.misw4203.vinilos.data

import co.edu.uniandes.misw4203.vinilos.data.model.Collector
import org.junit.Assert.assertEquals
import org.junit.Test

class CollectorTest {

    private fun collector(name: String) =
        Collector(id = 1, name = name, telephone = "", email = "")

    @Test
    fun `las iniciales son la primera letra de los dos primeros nombres`() {
        assertEquals("MB", collector("Manolo Bellon").initials)
    }

    @Test
    fun `un solo nombre en minuscula da una inicial en mayuscula`() {
        assertEquals("J", collector("jaime").initials)
    }

    @Test
    fun `los espacios de mas no cuentan`() {
        assertEquals("JM", collector("  Jaime   Monsalve  ").initials)
    }

    @Test
    fun `con mas de dos palabras solo se usan las dos primeras`() {
        assertEquals("AM", collector("Ana María Pérez").initials)
    }

    @Test
    fun `una inicial con tilde se conserva en mayuscula`() {
        assertEquals("ÁR", collector("álvaro ríos").initials)
    }

    @Test
    fun `sin nombre se muestra un signo de pregunta`() {
        assertEquals("?", collector("").initials)
        assertEquals("?", collector("   ").initials)
    }
}
