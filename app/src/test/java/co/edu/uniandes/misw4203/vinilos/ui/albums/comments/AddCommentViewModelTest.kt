package co.edu.uniandes.misw4203.vinilos.ui.albums.comments

import androidx.lifecycle.SavedStateHandle
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.albumDto
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.collectorDto
import co.edu.uniandes.misw4203.vinilos.data.adapter.AlbumServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.adapter.CollectorServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.repository.AlbumRepository
import co.edu.uniandes.misw4203.vinilos.data.repository.CollectorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/** HU09: ViewModel de la pantalla "Comentar álbum". */
@OptIn(ExperimentalCoroutinesApi::class)
class AddCommentViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        // viewModelScope usa Dispatchers.Main, que no existe en las pruebas locales
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun apiWith() = FakeBackendApi(
        albums = listOf(albumDto(id = 100)),
        collectors = listOf(
            collectorDto(id = 100, name = "Manolo Bellon"),
            collectorDto(id = 101, name = "Jaime Monsalve")
        )
    )

    private fun viewModelWith(
        api: FakeBackendApi = apiWith(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(mapOf(AddCommentViewModel.ARG_ALBUM_ID to 100))
    ) = AddCommentViewModel(
        savedStateHandle,
        AlbumRepository(AlbumServiceAdapter(api, dispatcher), dispatcher),
        CollectorRepository(CollectorServiceAdapter(api, dispatcher), dispatcher)
    )

    private fun AddCommentViewModel.fillValidForm(collectorId: Int = 100) {
        onCollectorSelected(collectorId)
        onRatingChanged(4)
        onDescriptionChanged("  Un álbum inolvidable  ")
    }

    @Test
    fun `carga el album y los coleccionistas`() = runTest(dispatcher) {
        val viewModel = viewModelWith()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Buscando América", state.album?.name)
        assertEquals(listOf(100, 101), state.collectors.map { it.id })
        assertFalse(state.isLoadingCollectors)
        assertFalse(state.collectorsLoadFailed)
    }

    @Test
    fun `publicar vacio muestra los tres errores y el aviso sin llamar al backend`() = runTest(dispatcher) {
        val api = apiWith()
        val viewModel = viewModelWith(api)
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(
            mapOf(
                CommentFormField.COLLECTOR to CommentFormError.REQUIRED,
                CommentFormField.RATING to CommentFormError.REQUIRED,
                CommentFormField.DESCRIPTION to CommentFormError.REQUIRED
            ),
            state.errors
        )
        assertTrue(state.showErrorBanner)
        assertTrue(api.addedComments.isEmpty())
    }

    @Test
    fun `un comentario de mas de 500 caracteres muestra TOO_LONG y no envia`() = runTest(dispatcher) {
        val api = apiWith()
        val viewModel = viewModelWith(api)
        advanceUntilIdle()
        viewModel.fillValidForm()
        viewModel.onDescriptionChanged("a".repeat(501))

        viewModel.save()
        advanceUntilIdle()

        assertEquals(
            mapOf(CommentFormField.DESCRIPTION to CommentFormError.TOO_LONG),
            viewModel.uiState.value.errors
        )
        assertTrue(api.addedComments.isEmpty())
    }

    @Test
    fun `corregir un campo quita su error`() = runTest(dispatcher) {
        val viewModel = viewModelWith()
        advanceUntilIdle()
        viewModel.save()

        viewModel.onCollectorSelected(100)
        assertFalse(CommentFormField.COLLECTOR in viewModel.uiState.value.errors)
        viewModel.onRatingChanged(3)
        assertFalse(CommentFormField.RATING in viewModel.uiState.value.errors)
        viewModel.onDescriptionChanged("Bien")

        val state = viewModel.uiState.value
        assertTrue(state.errors.isEmpty())
        assertFalse(state.showErrorBanner)
    }

    @Test
    fun `un envio valido emite Published y manda coleccionista calificacion y texto sin espacios`() =
        runTest(dispatcher) {
            val api = apiWith()
            val viewModel = viewModelWith(api)
            advanceUntilIdle()
            viewModel.fillValidForm(collectorId = 101)

            viewModel.save()
            advanceUntilIdle()

            assertEquals(AddCommentEvent.Published, viewModel.events.first())
            val body = api.addedComments.single()
            assertEquals(101, body.collector.id)
            assertEquals(4, body.rating)
            assertEquals("Un álbum inolvidable", body.description)
        }

    @Test
    fun `un fallo emite Failed y conserva los datos`() = runTest(dispatcher) {
        val api = apiWith()
        val viewModel = viewModelWith(api)
        advanceUntilIdle()
        viewModel.fillValidForm()
        api.error = IOException("sin red")

        viewModel.save()
        advanceUntilIdle()

        val event = viewModel.events.first()
        assertTrue(event is AddCommentEvent.Failed)
        assertTrue((event as AddCommentEvent.Failed).message.contains("conexión"))
        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertEquals(100, state.selectedCollectorId)
        assertEquals(4, state.rating)

        // El texto también se conserva: al reintentar no hay que volver a escribirlo
        api.error = null
        viewModel.save()
        advanceUntilIdle()
        assertEquals("Un álbum inolvidable", api.addedComments.last().description)
    }

    @Test
    fun `no hay doble envio mientras isSaving`() = runTest(dispatcher) {
        val api = apiWith()
        val viewModel = viewModelWith(api)
        advanceUntilIdle()
        viewModel.fillValidForm()

        viewModel.save()
        assertTrue(viewModel.uiState.value.isSaving)
        viewModel.save()
        advanceUntilIdle()

        assertEquals(1, api.addedComments.size)
    }

    @Test
    fun `si no cargan los coleccionistas el campo lo indica y se puede reintentar`() = runTest(dispatcher) {
        val api = apiWith()
        api.error = IOException("sin red")
        val viewModel = viewModelWith(api)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.collectorsLoadFailed)
        assertFalse(viewModel.uiState.value.isLoadingCollectors)

        api.error = null
        viewModel.retryCollectors()
        assertTrue(viewModel.uiState.value.isLoadingCollectors)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.collectorsLoadFailed)
        assertEquals(2, state.collectors.size)
    }

    @Test
    fun `restaura lo elegido y escrito desde SavedStateHandle`() = runTest(dispatcher) {
        val saved = SavedStateHandle(mapOf(AddCommentViewModel.ARG_ALBUM_ID to 100))
        val first = viewModelWith(savedStateHandle = saved)
        advanceUntilIdle()
        first.fillValidForm(collectorId = 101)
        first.onRatingChanged(2)

        // Un ViewModel nuevo con el mismo SavedStateHandle (rotación o muerte del proceso)
        val api = apiWith()
        val second = viewModelWith(api, saved)
        advanceUntilIdle()

        val state = second.uiState.value
        assertEquals(101, state.selectedCollectorId)
        assertEquals(2, state.rating)

        // El texto también se conserva: el formulario ya es válido sin volver a escribirlo
        second.save()
        advanceUntilIdle()
        val body = api.addedComments.single()
        assertEquals(101, body.collector.id)
        assertEquals(2, body.rating)
        assertEquals("Un álbum inolvidable", body.description)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `sin el argumento albumId no se puede crear`() {
        viewModelWith(savedStateHandle = SavedStateHandle())
    }
}
