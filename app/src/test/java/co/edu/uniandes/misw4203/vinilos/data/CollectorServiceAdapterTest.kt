package co.edu.uniandes.misw4203.vinilos.data

import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.albumDto
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.collectorAlbumDto
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.collectorDto
import co.edu.uniandes.misw4203.vinilos.data.adapter.CollectorServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.adapter.CollectorServiceAdapter.Companion.toDetailModel
import co.edu.uniandes.misw4203.vinilos.data.adapter.CollectorServiceAdapter.Companion.toModel
import co.edu.uniandes.misw4203.vinilos.data.dto.CommentDto
import co.edu.uniandes.misw4203.vinilos.data.dto.PerformerDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
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

    // HU06

    @Test
    fun `el listado no trae coleccion ni favoritos ni comentarios`() {
        val collector = collectorDto(
            comments = listOf(CommentDto(1, "x", 5)),
            favoritePerformers = listOf(PerformerDto(1, "Queen", "q.jpg"))
        ).toModel()

        assertTrue(collector!!.albums.isEmpty())
        assertTrue(collector.favoritePerformers.isEmpty())
        assertTrue(collector.comments.isEmpty())
    }

    @Test
    fun `el detalle convierte comentarios favoritos y coleccion`() {
        val collector = collectorDto(
            comments = listOf(CommentDto(100, "The most relevant album of Ruben Blades", 5)),
            favoritePerformers = listOf(PerformerDto(100, "Rubén Blades Bellido de Luna", "rb.jpg"))
        ).toDetailModel(listOf(collectorAlbumDto(id = 100, price = 35, status = "Active").toModel()!!))

        assertEquals("The most relevant album of Ruben Blades", collector?.comments?.single()?.description)
        assertEquals(5, collector?.comments?.single()?.rating)
        assertEquals("Rubén Blades Bellido de Luna", collector?.favoritePerformers?.single()?.name)
        val item = collector?.albums?.single()
        assertEquals(35, item?.price)
        assertEquals("Active", item?.status)
        assertEquals("Buscando América", item?.album?.name)
    }

    @Test
    fun `el detalle sin listas queda con listas vacias`() {
        val collector = collectorDto(comments = null, favoritePerformers = null).toDetailModel(emptyList())

        assertTrue(collector!!.albums.isEmpty())
        assertTrue(collector.favoritePerformers.isEmpty())
        assertTrue(collector.comments.isEmpty())
    }

    @Test
    fun `un detalle sin id se descarta`() {
        assertNull(collectorDto(id = null).toDetailModel(emptyList()))
    }

    @Test
    fun `un album de la coleccion sin id o sin album se descarta`() {
        assertNull(collectorAlbumDto(id = null).toModel())
        assertNull(collectorAlbumDto(album = null).toModel())
        assertNull(collectorAlbumDto(album = albumDto(id = null)).toModel())
    }

    @Test
    fun `precio y estado que no llegan quedan con valores por defecto`() {
        val item = collectorAlbumDto(price = null, status = null).toModel()

        assertEquals(0, item?.price)
        assertEquals("", item?.status)
    }

    @Test
    fun `getCollector une el detalle con los albumes de su coleccion`() = runBlocking {
        val adapter = CollectorServiceAdapter(
            FakeBackendApi(
                collectors = listOf(collectorDto(id = 100, name = "Manolo Bellon")),
                collectorAlbums = listOf(collectorAlbumDto(), collectorAlbumDto(id = null))
            )
        )

        val collector = adapter.getCollector(100)

        assertEquals("Manolo Bellon", collector.name)
        assertEquals(listOf("Buscando América"), collector.albums.map { it.album.name })
    }

    @Test
    fun `getCollector de un id sin coleccionista propaga el error`() {
        val adapter = CollectorServiceAdapter(FakeBackendApi())

        assertThrows(retrofit2.HttpException::class.java) { runBlocking { adapter.getCollector(999) } }
    }
}
