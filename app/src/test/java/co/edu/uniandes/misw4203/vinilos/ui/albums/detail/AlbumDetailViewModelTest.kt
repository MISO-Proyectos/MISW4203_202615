package co.edu.uniandes.misw4203.vinilos.ui.albums.detail

import androidx.lifecycle.SavedStateHandle
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.albumDto
import co.edu.uniandes.misw4203.vinilos.data.adapter.AlbumServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.dto.TrackCreateDto
import co.edu.uniandes.misw4203.vinilos.data.repository.AlbumRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AlbumDetailViewModelTest {

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

    private fun viewModelWith(api: FakeBackendApi, albumId: Int = 100) =
        AlbumDetailViewModel(
            SavedStateHandle(mapOf(AlbumDetailViewModel.ARG_ALBUM_ID to albumId)),
            AlbumRepository(AlbumServiceAdapter(api, dispatcher), dispatcher)
        )

    @Test
    fun `empieza en Loading mientras llega el album`() = runTest(dispatcher) {
        val viewModel = viewModelWith(FakeBackendApi(albums = listOf(albumDto(id = 100))))

        assertEquals(AlbumDetailUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `muestra el album del id recibido en la navegacion`() = runTest(dispatcher) {
        val api = FakeBackendApi(albums = listOf(albumDto(id = 100), albumDto(id = 101, name = "Mundo")))
        val viewModel = viewModelWith(api, albumId = 101)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AlbumDetailUiState.Success)
        assertEquals("Mundo", (state as AlbumDetailUiState.Success).album.name)
    }

    @Test
    fun `un album que no existe da Error con un mensaje entendible`() = runTest(dispatcher) {
        val viewModel = viewModelWith(FakeBackendApi(albums = emptyList()), albumId = 999)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AlbumDetailUiState.Error)
        assertTrue((state as AlbumDetailUiState.Error).message.contains("No encontramos"))
    }

    @Test
    fun `sin conexion da Error y reintentar vuelve a pedir el album`() = runTest(dispatcher) {
        val viewModel = viewModelWith(FakeBackendApi(error = IOException("sin red")))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is AlbumDetailUiState.Error)

        viewModel.loadAlbum()

        assertEquals(AlbumDetailUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `agregar comentario exitosamente recarga el album`() = runTest(dispatcher) {
        val api = FakeBackendApi(albums = listOf(albumDto(id = 100)))
        val viewModel = viewModelWith(api, albumId = 100)
        advanceUntilIdle()

        var resultSuccess = false
        viewModel.addComment(collectorId = 100, rating = 5, description = "Gran álbum") { result ->
            resultSuccess = result.isSuccess
        }
        advanceUntilIdle()

        assertTrue(resultSuccess)
        assertEquals(1, api.addedComments.size)
        assertEquals("Gran álbum", api.addedComments.first().description)
        assertEquals(5, api.addedComments.first().rating)
        assertTrue(viewModel.uiState.value is AlbumDetailUiState.Success)
    }

    @Test
    fun `error al agregar comentario notifica fallo`() = runTest(dispatcher) {
        val api = FakeBackendApi(albums = listOf(albumDto(id = 100)))
        val viewModel = viewModelWith(api, albumId = 100)
        advanceUntilIdle()

        api.error = IOException("sin red")
        var resultSuccess = true
        viewModel.addComment(collectorId = 100, rating = 4, description = "Error") { result ->
            resultSuccess = result.isSuccess
        }
        advanceUntilIdle()

        assertTrue(!resultSuccess)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `sin el argumento albumId no se puede crear`() {
        AlbumDetailViewModel(SavedStateHandle(), AlbumRepository(AlbumServiceAdapter(FakeBackendApi(), dispatcher), dispatcher))
    }

    // HU08: al volver del formulario con una canción agregada

    @Test
    fun `al agregar un track recarga sin pasar por Loading y lo resalta`() = runTest(dispatcher) {
        val api = FakeBackendApi(albums = listOf(albumDto(id = 100)))
        val viewModel = viewModelWith(api)
        advanceUntilIdle()
        val track = api.addTrack(100, TrackCreateDto("Todos vuelven", "05:03"))

        viewModel.onTrackAdded(track.id!!)
        // Mientras recarga se sigue viendo el álbum anterior
        assertTrue(viewModel.uiState.value is AlbumDetailUiState.Success)
        advanceUntilIdle()

        val state = viewModel.uiState.value as AlbumDetailUiState.Success
        assertEquals(listOf("Decisiones", "Todos vuelven"), state.album.tracks.map { it.name })
        assertEquals(track.id, state.highlightedTrackId)
    }

    @Test
    fun `si la recarga falla se deja el album que ya se veia`() = runTest(dispatcher) {
        val api = FakeBackendApi(albums = listOf(albumDto(id = 100)))
        val viewModel = viewModelWith(api)
        advanceUntilIdle()
        val before = viewModel.uiState.value

        api.error = IOException("sin red")
        viewModel.onTrackAdded(11)
        advanceUntilIdle()

        assertEquals(before, viewModel.uiState.value)
    }

    @Test
    fun `reintentar despues de agregar un track lo sigue resaltando`() = runTest(dispatcher) {
        val api = FakeBackendApi(albums = listOf(albumDto(id = 100)))
        val viewModel = viewModelWith(api)
        advanceUntilIdle()
        viewModel.onTrackAdded(10)
        advanceUntilIdle()

        viewModel.loadAlbum()
        advanceUntilIdle()

        assertEquals(10, (viewModel.uiState.value as AlbumDetailUiState.Success).highlightedTrackId)
    }

    @Test
    fun `al abrir el detalle ningun track esta resaltado`() = runTest(dispatcher) {
        val viewModel = viewModelWith(FakeBackendApi(albums = listOf(albumDto(id = 100))))
        advanceUntilIdle()

        assertEquals(null, (viewModel.uiState.value as AlbumDetailUiState.Success).highlightedTrackId)
    }
}
