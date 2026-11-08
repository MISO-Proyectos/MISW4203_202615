package co.edu.uniandes.misw4203.vinilos.ui.artists

import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.bandDto
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.musicianDto
import co.edu.uniandes.misw4203.vinilos.data.adapter.ArtistServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.repository.ArtistRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ArtistListViewModelTest {

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
        ArtistListViewModel(ArtistRepository(ArtistServiceAdapter(api)))

    @Test
    fun `empieza en Loading mientras llegan los artistas`() = runTest(dispatcher) {
        val viewModel = viewModelWith(FakeBackendApi(musicians = listOf(musicianDto())))

        assertEquals(ArtistListUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `muestra musicos y bandas cuando el backend responde`() = runTest(dispatcher) {
        val viewModel = viewModelWith(
            FakeBackendApi(musicians = listOf(musicianDto(id = 100)), bands = listOf(bandDto(id = 101)))
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ArtistListUiState.Success)
        assertEquals(listOf(100, 101), (state as ArtistListUiState.Success).artists.map { it.id })
    }

    @Test
    fun `sin artistas da el estado Empty`() = runTest(dispatcher) {
        val viewModel = viewModelWith(FakeBackendApi())
        advanceUntilIdle()

        assertEquals(ArtistListUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `sin conexion da Error con el mensaje del repositorio`() = runTest(dispatcher) {
        val viewModel = viewModelWith(FakeBackendApi(error = IOException("sin red")))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ArtistListUiState.Error)
        assertTrue((state as ArtistListUiState.Error).message.startsWith("No hay conexión"))
    }

    @Test
    fun `reintentar despues de un error vuelve a pedir los artistas`() = runTest(dispatcher) {
        val api = FakeBackendApi(musicians = listOf(musicianDto()), error = IOException("sin red"))
        val viewModel = viewModelWith(api)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is ArtistListUiState.Error)

        api.error = null
        viewModel.loadArtists()
        assertEquals(ArtistListUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is ArtistListUiState.Success)
    }

    @Test
    fun `al recargar la lista sigue visible y marcada como recargando`() = runTest(dispatcher) {
        val viewModel = viewModelWith(FakeBackendApi(musicians = listOf(musicianDto(id = 100))))
        advanceUntilIdle()

        viewModel.refresh()

        val state = viewModel.uiState.value as ArtistListUiState.Success
        assertTrue(state.isRefreshing)
        assertEquals(listOf(100), state.artists.map { it.id })
    }

    @Test
    fun `recargar trae los datos nuevos del backend`() = runTest(dispatcher) {
        val api = FakeBackendApi(musicians = listOf(musicianDto(id = 100)))
        val viewModel = viewModelWith(api)
        advanceUntilIdle()

        api.musicians = listOf(musicianDto(id = 100), musicianDto(id = 102, name = "Miles Davis"))
        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ArtistListUiState.Success
        assertFalse(state.isRefreshing)
        assertEquals(listOf(100, 102), state.artists.map { it.id })
    }

    @Test
    fun `si la recarga falla se conserva la lista y se avisa el error`() = runTest(dispatcher) {
        val api = FakeBackendApi(musicians = listOf(musicianDto(id = 100)))
        val viewModel = viewModelWith(api)
        advanceUntilIdle()

        api.error = IOException("sin red")
        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ArtistListUiState.Success
        assertFalse(state.isRefreshing)
        assertEquals(listOf(100), state.artists.map { it.id })
        assertTrue(state.refreshError.orEmpty().startsWith("No hay conexión"))

        viewModel.refreshErrorShown()
        assertNull((viewModel.uiState.value as ArtistListUiState.Success).refreshError)
    }

    @Test
    fun `recargar desde Error vuelve a cargar a pantalla completa`() = runTest(dispatcher) {
        val api = FakeBackendApi(musicians = listOf(musicianDto()), error = IOException("sin red"))
        val viewModel = viewModelWith(api)
        advanceUntilIdle()

        api.error = null
        viewModel.refresh()
        assertEquals(ArtistListUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is ArtistListUiState.Success)
    }
}
