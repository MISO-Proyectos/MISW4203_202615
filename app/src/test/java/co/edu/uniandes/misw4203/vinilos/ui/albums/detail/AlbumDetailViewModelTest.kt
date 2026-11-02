package co.edu.uniandes.misw4203.vinilos.ui.albums.detail

import androidx.lifecycle.SavedStateHandle
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.albumDto
import co.edu.uniandes.misw4203.vinilos.data.adapter.AlbumServiceAdapter
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
            AlbumRepository(AlbumServiceAdapter(api))
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

    @Test(expected = IllegalArgumentException::class)
    fun `sin el argumento albumId no se puede crear`() {
        AlbumDetailViewModel(SavedStateHandle(), AlbumRepository(AlbumServiceAdapter(FakeBackendApi())))
    }
}
