package co.edu.uniandes.misw4203.vinilos.ui.albums.create

import androidx.lifecycle.SavedStateHandle
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi
import co.edu.uniandes.misw4203.vinilos.data.adapter.AlbumServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.model.AlbumGenre
import co.edu.uniandes.misw4203.vinilos.data.model.RecordLabel
import co.edu.uniandes.misw4203.vinilos.data.model.ReleaseDate
import co.edu.uniandes.misw4203.vinilos.data.repository.AlbumRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/** HU07: ViewModel de la pantalla "Agregar álbum". */
@OptIn(ExperimentalCoroutinesApi::class)
class CreateAlbumViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val today = ReleaseDate(2026, 11, 15)

    @Before
    fun setUp() {
        // viewModelScope usa Dispatchers.Main, que no existe en las pruebas locales
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModelWith(
        api: FakeBackendApi = FakeBackendApi(),
        savedStateHandle: SavedStateHandle = SavedStateHandle()
    ) = CreateAlbumViewModel(
        savedStateHandle,
        AlbumRepository(AlbumServiceAdapter(api, dispatcher), dispatcher),
        today = { today }
    )

    private fun CreateAlbumViewModel.fillValidForm() {
        onNameChanged("Siembra")
        onCoverChanged("https://example.com/siembra.jpg")
        onReleaseDateSelected(ReleaseDate(1978, 8, 1))
        onGenreSelected(AlbumGenre.SALSA)
        onRecordLabelSelected(RecordLabel.FANIA)
        onDescriptionChanged("Un clásico de la salsa.")
    }

    @Test
    fun `empieza vacio y sin errores`() {
        val state = viewModelWith().uiState.value

        assertEquals(CreateAlbumUiState(), state)
    }

    @Test
    fun `guardar un formulario vacio muestra errores y no llama al backend`() = runTest(dispatcher) {
        val api = FakeBackendApi()
        val viewModel = viewModelWith(api)

        viewModel.save()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AlbumFormField.entries.toSet(), state.errors.keys)
        assertFalse(state.isSaving)
        assertTrue(api.createdAlbums.isEmpty())
    }

    @Test
    fun `una portada invalida muestra el error de enlace`() = runTest(dispatcher) {
        val viewModel = viewModelWith()
        viewModel.fillValidForm()
        viewModel.onCoverChanged("portada.jpg")

        viewModel.save()

        assertEquals(
            mapOf(AlbumFormField.COVER to AlbumFormError.INVALID_URL),
            viewModel.uiState.value.errors
        )
    }

    @Test
    fun `una fecha futura muestra el error de fecha`() = runTest(dispatcher) {
        val viewModel = viewModelWith()
        viewModel.fillValidForm()
        viewModel.onReleaseDateSelected(ReleaseDate(2030, 1, 1))

        viewModel.save()

        assertEquals(AlbumFormError.FUTURE_DATE, viewModel.uiState.value.errors[AlbumFormField.RELEASE_DATE])
    }

    @Test
    fun `al corregir un campo se borra solo su error`() = runTest(dispatcher) {
        val viewModel = viewModelWith()
        viewModel.save()

        viewModel.onNameChanged("Siembra")
        viewModel.onGenreSelected(AlbumGenre.SALSA)
        viewModel.onReleaseDateSelected(ReleaseDate(1978, 8, 1))

        val errors = viewModel.uiState.value.errors
        assertNull(errors[AlbumFormField.NAME])
        assertNull(errors[AlbumFormField.GENRE])
        assertNull(errors[AlbumFormField.RELEASE_DATE])
        assertEquals(
            setOf(AlbumFormField.COVER, AlbumFormField.RECORD_LABEL, AlbumFormField.DESCRIPTION),
            errors.keys
        )
    }

    @Test
    fun `volver a recibir el mismo texto no borra el error`() = runTest(dispatcher) {
        val viewModel = viewModelWith()
        viewModel.save()

        // Pasa al girar el teléfono: el campo restaura su texto (vacío) y avisa el cambio
        viewModel.onNameChanged("")

        assertEquals(AlbumFormError.REQUIRED, viewModel.uiState.value.errors[AlbumFormField.NAME])
    }

    @Test
    fun `un formulario valido crea el album y avisa Created`() = runTest(dispatcher) {
        val api = FakeBackendApi()
        val viewModel = viewModelWith(api)
        viewModel.fillValidForm()

        viewModel.save()
        assertTrue(viewModel.uiState.value.isSaving)
        val event = viewModel.events.first()

        assertEquals(CreateAlbumEvent.Created("Siembra"), event)
        assertFalse(viewModel.uiState.value.isSaving)
        val sent = api.createdAlbums.single()
        assertEquals("Siembra", sent.name)
        assertEquals("https://example.com/siembra.jpg", sent.cover)
        assertEquals("1978-08-01T00:00:00.000Z", sent.releaseDate)
        assertEquals("Salsa", sent.genre)
        assertEquals("Fania Records", sent.recordLabel)
        assertEquals("Un clásico de la salsa.", sent.description)
    }

    @Test
    fun `si el backend falla avisa Failed con un mensaje y deja editar de nuevo`() = runTest(dispatcher) {
        val viewModel = viewModelWith(FakeBackendApi(error = IOException("sin red")))
        viewModel.fillValidForm()

        viewModel.save()
        val event = viewModel.events.first()

        assertTrue(event is CreateAlbumEvent.Failed)
        assertTrue((event as CreateAlbumEvent.Failed).message.contains("No hay conexión"))
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `tocar guardar dos veces seguidas crea un solo album`() = runTest(dispatcher) {
        val api = FakeBackendApi()
        val viewModel = viewModelWith(api)
        viewModel.fillValidForm()

        viewModel.save()
        viewModel.save()
        advanceUntilIdle()

        assertEquals(1, api.createdAlbums.size)
    }

    @Test
    fun `los valores elegidos sobreviven a que Android cierre el proceso`() = runTest(dispatcher) {
        val savedStateHandle = SavedStateHandle()
        viewModelWith(savedStateHandle = savedStateHandle).fillValidForm()

        // Un ViewModel nuevo con el mismo SavedStateHandle simula la restauración
        val api = FakeBackendApi()
        val restored = viewModelWith(api, savedStateHandle)

        val state = restored.uiState.value
        assertEquals(ReleaseDate(1978, 8, 1), state.releaseDate)
        assertEquals(AlbumGenre.SALSA, state.genre)
        assertEquals(RecordLabel.FANIA, state.recordLabel)

        restored.save()
        advanceUntilIdle()
        assertEquals("Siembra", api.createdAlbums.single().name)
    }
}
