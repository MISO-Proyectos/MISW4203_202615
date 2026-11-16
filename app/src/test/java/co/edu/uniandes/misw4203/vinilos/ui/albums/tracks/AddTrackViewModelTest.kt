package co.edu.uniandes.misw4203.vinilos.ui.albums.tracks

import androidx.lifecycle.SavedStateHandle
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.albumDto
import co.edu.uniandes.misw4203.vinilos.data.adapter.AlbumServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.dto.TrackCreateDto
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

/** HU08: ViewModel de la pantalla "Agregar track". */
@OptIn(ExperimentalCoroutinesApi::class)
class AddTrackViewModelTest {

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

    private fun viewModelWith(
        api: FakeBackendApi = FakeBackendApi(albums = listOf(albumDto(id = 100))),
        savedStateHandle: SavedStateHandle =
            SavedStateHandle(mapOf(AddTrackViewModel.ARG_ALBUM_ID to 100))
    ) = AddTrackViewModel(
        savedStateHandle,
        AlbumRepository(AlbumServiceAdapter(api, dispatcher), dispatcher)
    )

    @Test
    fun `carga el album para la tarjeta superior`() = runTest(dispatcher) {
        val viewModel = viewModelWith()
        advanceUntilIdle()

        val album = viewModel.uiState.value.album
        assertEquals("Buscando América", album?.name)
        assertEquals(1, album?.tracks?.size)
    }

    @Test
    fun `si el album no carga el formulario sigue disponible sin tarjeta`() = runTest(dispatcher) {
        val viewModel = viewModelWith(FakeBackendApi(error = IOException("sin red")))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.album)
        assertTrue(state.errors.isEmpty())
        assertFalse(state.isSaving)
    }

    @Test
    fun `guardar vacio muestra errores y el aviso sin llamar al backend`() = runTest(dispatcher) {
        val api = FakeBackendApi(albums = listOf(albumDto(id = 100)))
        val viewModel = viewModelWith(api)

        viewModel.save()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(
            mapOf(
                TrackFormField.NAME to TrackFormError.REQUIRED,
                TrackFormField.DURATION to TrackFormError.INVALID_DURATION
            ),
            state.errors
        )
        assertTrue(state.showErrorBanner)
        assertTrue(api.addedTracks.isEmpty())
    }

    @Test
    fun `una duracion invalida solo marca la duracion`() = runTest(dispatcher) {
        val viewModel = viewModelWith()
        viewModel.onNameChanged("Todos vuelven")
        viewModel.onDurationChanged("5:75")

        viewModel.save()

        assertEquals(
            mapOf(TrackFormField.DURATION to TrackFormError.INVALID_DURATION),
            viewModel.uiState.value.errors
        )
    }

    @Test
    fun `al corregir los campos se borran sus errores y el aviso`() = runTest(dispatcher) {
        val viewModel = viewModelWith()
        viewModel.save()

        viewModel.onNameChanged("Todos vuelven")
        assertEquals(setOf(TrackFormField.DURATION), viewModel.uiState.value.errors.keys)
        assertTrue(viewModel.uiState.value.showErrorBanner)

        viewModel.onDurationChanged("5:03")
        assertTrue(viewModel.uiState.value.errors.isEmpty())
        assertFalse(viewModel.uiState.value.showErrorBanner)
    }

    @Test
    fun `volver a recibir el mismo texto no borra el error`() = runTest(dispatcher) {
        val viewModel = viewModelWith()
        viewModel.save()

        // Pasa al girar el teléfono: el campo restaura su texto (vacío) y avisa el cambio
        viewModel.onNameChanged("")

        assertEquals(TrackFormError.REQUIRED, viewModel.uiState.value.errors[TrackFormField.NAME])
    }

    @Test
    fun `un formulario valido guarda la cancion y avisa Added`() = runTest(dispatcher) {
        val api = FakeBackendApi(albums = listOf(albumDto(id = 100)))
        val viewModel = viewModelWith(api)
        viewModel.onNameChanged("  Todos vuelven ")
        viewModel.onDurationChanged("5:03")

        viewModel.save()
        assertTrue(viewModel.uiState.value.isSaving)
        val event = viewModel.events.first()

        assertEquals(AddTrackEvent.Added(trackId = 11, trackName = "Todos vuelven"), event)
        assertFalse(viewModel.uiState.value.isSaving)
        // La duración se envía normalizada como MM:SS
        assertEquals(listOf(100 to TrackCreateDto("Todos vuelven", "05:03")), api.addedTracks)
    }

    @Test
    fun `si el backend falla avisa Failed con un mensaje y deja editar de nuevo`() = runTest(dispatcher) {
        val api = FakeBackendApi(albums = listOf(albumDto(id = 100)))
        val viewModel = viewModelWith(api)
        advanceUntilIdle()
        viewModel.onNameChanged("Todos vuelven")
        viewModel.onDurationChanged("05:03")
        api.error = IOException("sin red")

        viewModel.save()
        val event = viewModel.events.first()

        assertTrue(event is AddTrackEvent.Failed)
        assertTrue((event as AddTrackEvent.Failed).message.contains("No hay conexión"))
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `tocar guardar dos veces seguidas agrega una sola cancion`() = runTest(dispatcher) {
        val api = FakeBackendApi(albums = listOf(albumDto(id = 100)))
        val viewModel = viewModelWith(api)
        viewModel.onNameChanged("Todos vuelven")
        viewModel.onDurationChanged("05:03")

        viewModel.save()
        viewModel.save()
        advanceUntilIdle()

        assertEquals(1, api.addedTracks.size)
    }

    @Test
    fun `lo escrito sobrevive a que Android cierre el proceso`() = runTest(dispatcher) {
        val savedStateHandle = SavedStateHandle(mapOf(AddTrackViewModel.ARG_ALBUM_ID to 100))
        viewModelWith(savedStateHandle = savedStateHandle).apply {
            onNameChanged("Todos vuelven")
            onDurationChanged("05:03")
        }

        // Un ViewModel nuevo con el mismo SavedStateHandle simula la restauración
        val api = FakeBackendApi(albums = listOf(albumDto(id = 100)))
        val restored = viewModelWith(api, savedStateHandle)
        restored.save()
        advanceUntilIdle()

        assertEquals(listOf(100 to TrackCreateDto("Todos vuelven", "05:03")), api.addedTracks)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `sin el argumento albumId no se puede crear`() {
        viewModelWith(savedStateHandle = SavedStateHandle())
    }
}
