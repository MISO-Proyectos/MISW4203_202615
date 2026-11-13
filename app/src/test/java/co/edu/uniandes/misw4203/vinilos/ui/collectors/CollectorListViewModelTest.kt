package co.edu.uniandes.misw4203.vinilos.ui.collectors

import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.collectorDto
import co.edu.uniandes.misw4203.vinilos.data.adapter.CollectorServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.repository.CollectorRepository
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
class CollectorListViewModelTest {

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
        CollectorListViewModel(CollectorRepository(CollectorServiceAdapter(api, dispatcher), dispatcher))

    @Test
    fun `empieza en Loading mientras llegan los coleccionistas`() = runTest(dispatcher) {
        val viewModel = viewModelWith(FakeBackendApi(collectors = listOf(collectorDto())))

        assertEquals(CollectorListUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `muestra los coleccionistas cuando el backend responde`() = runTest(dispatcher) {
        val viewModel = viewModelWith(
            FakeBackendApi(collectors = listOf(collectorDto(id = 100), collectorDto(id = 101)))
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CollectorListUiState.Success)
        assertEquals(listOf(100, 101), (state as CollectorListUiState.Success).collectors.map { it.id })
    }

    @Test
    fun `sin coleccionistas da el estado Empty`() = runTest(dispatcher) {
        val viewModel = viewModelWith(FakeBackendApi(collectors = emptyList()))
        advanceUntilIdle()

        assertEquals(CollectorListUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `sin conexion da Error con el mensaje del repositorio`() = runTest(dispatcher) {
        val viewModel = viewModelWith(FakeBackendApi(error = IOException("sin red")))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CollectorListUiState.Error)
        assertTrue((state as CollectorListUiState.Error).message.startsWith("No hay conexión"))
    }

    @Test
    fun `reintentar despues de un error vuelve a pedir los coleccionistas`() = runTest(dispatcher) {
        val api = FakeBackendApi(collectors = listOf(collectorDto()), error = IOException("sin red"))
        val viewModel = viewModelWith(api)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is CollectorListUiState.Error)

        api.error = null
        viewModel.loadCollectors()
        assertEquals(CollectorListUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is CollectorListUiState.Success)
    }
}
