package co.edu.uniandes.misw4203.vinilos.data

import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.albumDto
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.collectorDto
import co.edu.uniandes.misw4203.vinilos.data.adapter.AlbumServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.adapter.AlbumServiceAdapter.Companion.toDto
import co.edu.uniandes.misw4203.vinilos.data.dto.CollectorRefDto
import co.edu.uniandes.misw4203.vinilos.data.dto.CommentCreateDto
import co.edu.uniandes.misw4203.vinilos.data.model.Comment
import co.edu.uniandes.misw4203.vinilos.data.model.NewComment
import co.edu.uniandes.misw4203.vinilos.data.repository.AlbumRepository
import co.edu.uniandes.misw4203.vinilos.data.repository.RepositoryException
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

/** HU09: publicar un comentario en un álbum, en el Service Adapter y en el repositorio. */
@OptIn(ExperimentalCoroutinesApi::class)
class AlbumAddCommentTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private val newComment = NewComment(collectorId = 100, rating = 4, description = "  Muy bueno ")

    private fun apiWithAlbum() = FakeBackendApi(
        albums = listOf(albumDto(id = 100)),
        collectors = listOf(collectorDto(id = 100))
    )

    private fun repositoryWith(api: FakeBackendApi) =
        AlbumRepository(AlbumServiceAdapter(api, dispatcher), dispatcher)

    @Test
    fun `el cuerpo lleva texto sin espacios sobrantes, calificacion y coleccionista`() {
        assertEquals(
            CommentCreateDto("Muy bueno", 4, CollectorRefDto(100)),
            newComment.toDto()
        )
    }

    @Test
    fun `el adapter envia el comentario al album y devuelve el creado con su id`() = runBlocking {
        val api = apiWithAlbum()

        val comment = AlbumServiceAdapter(api, dispatcher).addComment(100, newComment)

        // albumDto ya trae el comentario 1, así que el nuevo recibe el 2
        assertEquals(Comment(2, "Muy bueno", 4), comment)
        assertEquals(listOf(CommentCreateDto("Muy bueno", 4, CollectorRefDto(100))), api.addedComments)
    }

    @Test
    fun `el repositorio devuelve el comentario creado`() = runBlocking {
        val result = repositoryWith(apiWithAlbum()).addComment(100, newComment)

        assertTrue(result.isSuccess)
        assertEquals("Muy bueno", result.getOrThrow().description)
    }

    @Test
    fun `al volver a pedir el detalle ya trae el comentario nuevo`() = runBlocking {
        val repository = repositoryWith(apiWithAlbum())

        repository.addComment(100, newComment)
        val album = repository.getAlbum(100).getOrThrow()

        assertEquals(listOf("Un clásico", "Muy bueno"), album.comments.map { it.description })
    }

    @Test
    fun `si el album no existe devuelve NotFound`() = runBlocking {
        val error = repositoryWith(apiWithAlbum()).addComment(999, newComment).exceptionOrNull()

        assertTrue(error is RepositoryException.NotFound)
    }

    @Test
    fun `si el coleccionista no existe devuelve NotFound`() = runBlocking {
        val error = repositoryWith(apiWithAlbum())
            .addComment(100, newComment.copy(collectorId = 999))
            .exceptionOrNull()

        assertTrue(error is RepositoryException.NotFound)
    }

    @Test
    fun `sin conexion devuelve NoConnection`() = runBlocking {
        val error = repositoryWith(FakeBackendApi(error = IOException("timeout")))
            .addComment(100, newComment)
            .exceptionOrNull()

        assertTrue(error is RepositoryException.NoConnection)
    }

    @Test
    fun `si el backend rechaza el comentario devuelve Server con el codigo`() = runBlocking {
        val httpError = HttpException(Response.error<Any>(412, "".toResponseBody(null)))

        val error = repositoryWith(FakeBackendApi(error = httpError))
            .addComment(100, newComment)
            .exceptionOrNull()

        assertTrue(error is RepositoryException.Server)
        assertEquals(412, (error as RepositoryException.Server).code)
    }
}
