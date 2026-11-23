package co.edu.uniandes.misw4203.vinilos.data

import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.collectorAlbumDto
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.bandDto
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.collectorDto
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.musicianDto
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.albumDto
import co.edu.uniandes.misw4203.vinilos.data.adapter.CollectorServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.dto.PerformerDto
import co.edu.uniandes.misw4203.vinilos.data.model.ArtistType
import co.edu.uniandes.misw4203.vinilos.data.model.CollectionResult
import co.edu.uniandes.misw4203.vinilos.data.model.CollectorAlbumStatus
import co.edu.uniandes.misw4203.vinilos.data.model.FavoriteResult
import co.edu.uniandes.misw4203.vinilos.data.repository.CollectorRepository
import co.edu.uniandes.misw4203.vinilos.data.repository.RepositoryException
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CollectorRepositoryTest {

    // Se ejecuta de inmediato en el hilo de la prueba: sin hilos reales ni esperas
    private val dispatcher = UnconfinedTestDispatcher()

    private fun repositoryWith(api: FakeBackendApi) =
        CollectorRepository(CollectorServiceAdapter(api, dispatcher), dispatcher)

    @Test
    fun `devuelve los coleccionistas cuando el backend responde`() = runBlocking {
        val result = repositoryWith(
            FakeBackendApi(collectors = listOf(collectorDto(id = 100), collectorDto(id = 101)))
        ).getCollectors()

        assertTrue(result.isSuccess)
        assertEquals(listOf(100, 101), result.getOrThrow().map { it.id })
    }

    @Test
    fun `descarta los coleccionistas que llegan sin id`() = runBlocking {
        val result = repositoryWith(
            FakeBackendApi(collectors = listOf(collectorDto(id = null), collectorDto(id = 101)))
        ).getCollectors()

        assertEquals(listOf(101), result.getOrThrow().map { it.id })
    }

    @Test
    fun `una lista vacia es un resultado exitoso`() = runBlocking {
        val result = repositoryWith(FakeBackendApi(collectors = emptyList())).getCollectors()

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
    }

    @Test
    fun `sin conexion devuelve NoConnection`() = runBlocking {
        val result = repositoryWith(FakeBackendApi(error = IOException("timeout"))).getCollectors()

        assertTrue(result.exceptionOrNull() is RepositoryException.NoConnection)
    }

    @Test
    fun `un error HTTP devuelve Server con el codigo`() = runBlocking {
        val httpError = HttpException(Response.error<Any>(500, "".toResponseBody(null)))

        val error = repositoryWith(FakeBackendApi(error = httpError)).getCollectors().exceptionOrNull()

        assertTrue(error is RepositoryException.Server)
        assertEquals(500, (error as RepositoryException.Server).code)
    }

    @Test
    fun `un JSON invalido devuelve Unexpected`() = runBlocking {
        val result = repositoryWith(FakeBackendApi(error = JsonSyntaxException("bad json"))).getCollectors()

        assertTrue(result.exceptionOrNull() is RepositoryException.Unexpected)
    }

    @Test
    fun `los errores traen un mensaje entendible para el usuario`() = runBlocking {
        val error = repositoryWith(FakeBackendApi(error = IOException())).getCollectors().exceptionOrNull()

        assertTrue(error?.message.orEmpty().contains("conexión"))
    }

    // HU06

    @Test
    fun `getCollector devuelve el coleccionista con su coleccion`() = runBlocking {
        val result = repositoryWith(
            FakeBackendApi(
                collectors = listOf(collectorDto(id = 100)),
                collectorAlbums = listOf(collectorAlbumDto())
            )
        ).getCollector(100)

        assertTrue(result.isSuccess)
        assertEquals(100, result.getOrThrow().id)
        assertEquals(1, result.getOrThrow().albums.size)
    }

    @Test
    fun `getCollector de un coleccionista que no existe devuelve NotFound`() = runBlocking {
        val error = repositoryWith(FakeBackendApi()).getCollector(999).exceptionOrNull()

        assertTrue(error is RepositoryException.NotFound)
        assertTrue(error?.message.orEmpty().contains("No encontramos este coleccionista"))
    }

    @Test
    fun `getCollector sin conexion devuelve NoConnection`() = runBlocking {
        val result = repositoryWith(FakeBackendApi(error = IOException("timeout"))).getCollector(100)

        assertTrue(result.exceptionOrNull() is RepositoryException.NoConnection)
    }

    @Test
    fun `getCollector con error HTTP 500 devuelve Server con el codigo`() = runBlocking {
        val httpError = HttpException(Response.error<Any>(500, "".toResponseBody(null)))
        val error = repositoryWith(FakeBackendApi(error = httpError)).getCollector(100).exceptionOrNull()

        assertTrue(error is RepositoryException.Server)
        assertEquals(500, (error as RepositoryException.Server).code)
    }

    @Test
    fun `getCollector con JSON invalido devuelve Unexpected`() = runBlocking {
        val result = repositoryWith(FakeBackendApi(error = JsonSyntaxException("bad json"))).getCollector(100)

        assertTrue(result.exceptionOrNull() is RepositoryException.Unexpected)
    }

    // HU11

    private fun apiWithAlbumAndCollector(inCollection: Boolean = false) = FakeBackendApi(
        albums = listOf(albumDto(id = 100)),
        collectors = listOf(collectorDto(id = 100)),
        collectorAlbums = if (inCollection) listOf(collectorAlbumDto(album = albumDto(id = 100))) else emptyList()
    )

    @Test
    fun `addAlbumToCollector agrega el album con su precio y estado`() = runBlocking {
        val api = apiWithAlbumAndCollector()

        val result = repositoryWith(api).addAlbumToCollector(100, 100, 35000, CollectorAlbumStatus.INACTIVE)

        assertEquals(CollectionResult.ADDED, result.getOrThrow())
        val (collectorId, albumId, body) = api.addedToCollection.single()
        assertEquals(100, collectorId)
        assertEquals(100, albumId)
        assertEquals(35000, body.price)
        assertEquals("Inactive", body.status)
    }

    @Test
    fun `addAlbumToCollector no hace el POST si el album ya esta en la coleccion`() = runBlocking {
        val api = apiWithAlbumAndCollector(inCollection = true)

        val result = repositoryWith(api).addAlbumToCollector(100, 100, 35000, CollectorAlbumStatus.ACTIVE)

        assertEquals(CollectionResult.ALREADY_IN_COLLECTION, result.getOrThrow())
        assertTrue(api.addedToCollection.isEmpty())
    }

    @Test
    fun `addAlbumToCollector sin conexion devuelve NoConnection`() = runBlocking {
        val result = repositoryWith(FakeBackendApi(error = IOException("timeout")))
            .addAlbumToCollector(100, 100, 35000, CollectorAlbumStatus.ACTIVE)

        assertTrue(result.exceptionOrNull() is RepositoryException.NoConnection)
    }

    @Test
    fun `addAlbumToCollector con coleccionista inexistente devuelve NotFound con su mensaje`() = runBlocking {
        val error = repositoryWith(FakeBackendApi(albums = listOf(albumDto(id = 100))))
            .addAlbumToCollector(999, 100, 35000, CollectorAlbumStatus.ACTIVE).exceptionOrNull()

        assertTrue(error is RepositoryException.NotFound)
        assertEquals(
            "No encontramos el álbum o el coleccionista. Puede que hayan sido eliminados.",
            error?.message
        )
    }

    @Test
    fun `addAlbumToCollector con JSON invalido devuelve Unexpected con el mensaje de la accion`() = runBlocking {
        val error = repositoryWith(FakeBackendApi(error = JsonSyntaxException("bad json")))
            .addAlbumToCollector(100, 100, 35000, CollectorAlbumStatus.ACTIVE).exceptionOrNull()

        assertTrue(error is RepositoryException.Unexpected)
        assertEquals("No pudimos agregar el álbum a la colección. Intenta de nuevo.", error?.message)
    }

    @Test
    fun `al agregar con exito el album aparece en el detalle del coleccionista`() = runBlocking {
        val repository = repositoryWith(apiWithAlbumAndCollector())

        repository.addAlbumToCollector(100, 100, 35000, CollectorAlbumStatus.ACTIVE)

        val collector = repository.getCollector(100).getOrThrow()
        assertEquals(35000, collector.albums.single().price)
        assertEquals("Active", collector.albums.single().status)
    }

    // HU10

    private fun apiWithArtistAndCollector(alreadyFavorite: Boolean = false) = FakeBackendApi(
        collectors = listOf(
            collectorDto(
                id = 100,
                favoritePerformers = if (alreadyFavorite) listOf(PerformerDto(100, "Rubén Blades", "rb.jpg")) else null
            )
        ),
        musicians = listOf(musicianDto(id = 100)),
        bands = listOf(bandDto(id = 101))
    )

    @Test
    fun `addFavoriteArtist agrega un musico a los favoritos`() = runBlocking {
        val api = apiWithArtistAndCollector()

        val result = repositoryWith(api).addFavoriteArtist(100, 100, ArtistType.MUSICIAN)

        assertEquals(FavoriteResult.ADDED, result.getOrThrow())
        assertEquals(listOf(Triple(100, 100, ArtistType.MUSICIAN)), api.addedFavorites)
    }

    @Test
    fun `addFavoriteArtist agrega una banda a los favoritos`() = runBlocking {
        val api = apiWithArtistAndCollector()

        val result = repositoryWith(api).addFavoriteArtist(100, 101, ArtistType.BAND)

        assertEquals(FavoriteResult.ADDED, result.getOrThrow())
        assertEquals(listOf(Triple(100, 101, ArtistType.BAND)), api.addedFavorites)
    }

    @Test
    fun `addFavoriteArtist no hace el POST si el artista ya es favorito del coleccionista`() = runBlocking {
        val api = apiWithArtistAndCollector(alreadyFavorite = true)

        val result = repositoryWith(api).addFavoriteArtist(100, 100, ArtistType.MUSICIAN)

        assertEquals(FavoriteResult.ALREADY_FAVORITE, result.getOrThrow())
        assertTrue(api.addedFavorites.isEmpty())
    }

    @Test
    fun `addFavoriteArtist solo avisa duplicado para el coleccionista que ya lo tiene`() = runBlocking {
        val api = apiWithArtistAndCollector(alreadyFavorite = true).apply {
            collectors = collectors + collectorDto(id = 101, name = "Otro", email = "otro@correo.com")
        }

        val result = repositoryWith(api).addFavoriteArtist(101, 100, ArtistType.MUSICIAN)

        assertEquals(FavoriteResult.ADDED, result.getOrThrow())
    }

    @Test
    fun `addFavoriteArtist con coleccionista inexistente devuelve NotFound con su mensaje`() = runBlocking {
        val api = apiWithArtistAndCollector()

        val error = repositoryWith(api).addFavoriteArtist(999, 100, ArtistType.MUSICIAN).exceptionOrNull()

        assertTrue(error is RepositoryException.NotFound)
        assertEquals(
            "No encontramos el artista o el coleccionista. Puede que hayan sido eliminados.",
            error?.message
        )
        assertTrue(api.addedFavorites.isEmpty())
    }

    @Test
    fun `addFavoriteArtist con artista inexistente devuelve NotFound`() = runBlocking {
        val error = repositoryWith(apiWithArtistAndCollector())
            .addFavoriteArtist(100, 999, ArtistType.MUSICIAN).exceptionOrNull()

        assertTrue(error is RepositoryException.NotFound)
    }

    @Test
    fun `addFavoriteArtist sin conexion devuelve NoConnection`() = runBlocking {
        val result = repositoryWith(FakeBackendApi(error = IOException("timeout")))
            .addFavoriteArtist(100, 100, ArtistType.MUSICIAN)

        assertTrue(result.exceptionOrNull() is RepositoryException.NoConnection)
    }

    @Test
    fun `addFavoriteArtist con error HTTP 500 devuelve Server con el codigo`() = runBlocking {
        val error = repositoryWith(
            FakeBackendApi(error = HttpException(Response.error<Any>(500, "".toResponseBody(null))))
        ).addFavoriteArtist(100, 100, ArtistType.MUSICIAN).exceptionOrNull()

        assertTrue(error is RepositoryException.Server)
        assertEquals(500, (error as RepositoryException.Server).code)
    }

    @Test
    fun `addFavoriteArtist con JSON invalido devuelve Unexpected con el mensaje de la accion`() = runBlocking {
        val error = repositoryWith(FakeBackendApi(error = JsonSyntaxException("bad json")))
            .addFavoriteArtist(100, 100, ArtistType.MUSICIAN).exceptionOrNull()

        assertTrue(error is RepositoryException.Unexpected)
        assertEquals("No pudimos agregar el artista a favoritos. Intenta de nuevo.", error?.message)
    }

    @Test
    fun `al agregar un favorito con exito un segundo intento se detecta como duplicado`() = runBlocking {
        val api = apiWithArtistAndCollector()
        val repository = repositoryWith(api)

        repository.addFavoriteArtist(100, 100, ArtistType.MUSICIAN)
        val second = repository.addFavoriteArtist(100, 100, ArtistType.MUSICIAN)

        assertEquals(FavoriteResult.ALREADY_FAVORITE, second.getOrThrow())
        assertEquals(1, api.addedFavorites.size)
    }

    @Test
    fun `al agregar un favorito con exito aparece en el detalle del coleccionista`() = runBlocking {
        val repository = repositoryWith(apiWithArtistAndCollector())

        repository.addFavoriteArtist(100, 100, ArtistType.MUSICIAN)

        assertEquals(listOf(100), repository.getCollector(100).getOrThrow().favoritePerformers.map { it.id })
    }
}
