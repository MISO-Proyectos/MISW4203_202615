package co.edu.uniandes.misw4203.vinilos.data

import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.collectorDto
import co.edu.uniandes.misw4203.vinilos.data.adapter.CollectorServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.adapter.CollectorServiceAdapter.Companion.toModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectorServiceAdapterTest {

    @Test
    fun `convierte el JSON en un coleccionista con todos sus datos`() {
        val collector = collectorDto(
            id = 100,
            name = "Manolo Bellon",
            telephone = "3502457896",
            email = "manollo@caracol.com.co"
        ).toModel()

        assertEquals(100, collector?.id)
        assertEquals("Manolo Bellon", collector?.name)
        assertEquals("3502457896", collector?.telephone)
        assertEquals("manollo@caracol.com.co", collector?.email)
    }

    @Test
    fun `un coleccionista sin id se descarta`() {
        assertNull(collectorDto(id = null).toModel())
    }

    @Test
    fun `los textos que no llegan quedan vacios en vez de null`() {
        val collector = collectorDto(name = null, telephone = null, email = null).toModel()

        assertEquals("", collector?.name)
        assertEquals("", collector?.telephone)
        assertEquals("", collector?.email)
    }

    @Test
    fun `getCollectors conserva el orden del backend y descarta los que no tienen id`() = runBlocking {
        val adapter = CollectorServiceAdapter(
            FakeBackendApi(
                collectors = listOf(
                    collectorDto(id = 101, name = "Jaime Monsalve"),
                    collectorDto(id = null, name = "Sin id"),
                    collectorDto(id = 100, name = "Manolo Bellon")
                )
            )
        )

        val result = adapter.getCollectors()

        assertEquals(listOf(101, 100), result.map { it.id })
        assertTrue(result.none { it.name == "Sin id" })
    }
}
