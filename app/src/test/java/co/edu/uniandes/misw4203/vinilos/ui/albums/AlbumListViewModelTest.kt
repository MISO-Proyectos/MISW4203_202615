package co.edu.uniandes.misw4203.vinilos.ui.albums

import co.edu.uniandes.misw4203.vinilos.core.network.BackendApi
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.albumDto
import co.edu.uniandes.misw4203.vinilos.data.adapter.AlbumServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.repository.AlbumRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
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
class AlbumListViewModelTest {

    private val dispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        // viewModelScope usa Dispatchers.Main, que no existe en las pruebas locales
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModelWith(api: FakeBackendApi) =
        AlbumListViewModel(AlbumRepository(AlbumServiceAdapter(api)))

    @Test
    fun `empieza en Loading mientras llegan los albumes`() = runTest(dispatcher) {
        val viewModel = viewModelWith(FakeBackendApi(albums = listOf(albumDto())))

        assertEquals(AlbumListUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `muestra los albumes cuando el backend responde`() = runTest(dispatcher) {
        val viewModel = viewModelWith(
            FakeBackendApi(albums = listOf(albumDto(id = 1), albumDto(id = 2)))
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AlbumListUiState.Success)
        assertEquals(listOf(1, 2), (state as AlbumListUiState.Success).albums.map { it.id })
    }

    @Test
    fun `un catalogo vacio da el estado Empty`() = runTest(dispatcher) {
        val viewModel = viewModelWith(FakeBackendApi(albums = emptyList()))
        advanceUntilIdle()

        assertEquals(AlbumListUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `sin conexion da Error con el mensaje del repositorio`() = runTest(dispatcher) {
        val viewModel = viewModelWith(FakeBackendApi(error = IOException("sin red")))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AlbumListUiState.Error)
        assertTrue((state as AlbumListUiState.Error).message.startsWith("No hay conexión"))
    }

    @Test
    fun `reintentar despues de un error vuelve a pedir los albumes`() = runTest(dispatcher) {
        val api = SwitchableApi()
        val viewModel = AlbumListViewModel(AlbumRepository(AlbumServiceAdapter(api)))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is AlbumListUiState.Error)

        api.online = true
        viewModel.loadAlbums()
        assertEquals(AlbumListUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AlbumListUiState.Success)
    }

    /** API que falla hasta que se pone [online] en true. */
    private class SwitchableApi : BackendApi {
        var online = false
        private val delegate = FakeBackendApi(albums = listOf(albumDto()))

        override suspend fun getAlbums() =
            if (online) delegate.getAlbums() else throw IOException("sin red")

        override suspend fun getAlbum(albumId: Int) = delegate.getAlbum(albumId)
    }
}
