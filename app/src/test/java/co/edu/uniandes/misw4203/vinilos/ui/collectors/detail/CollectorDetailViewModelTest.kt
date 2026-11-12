package co.edu.uniandes.misw4203.vinilos.ui.collectors.detail

import androidx.lifecycle.SavedStateHandle
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.collectorAlbumDto
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.collectorDto
import co.edu.uniandes.misw4203.vinilos.data.adapter.CollectorServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.repository.CollectorRepository
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
class CollectorDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModelWith(api: FakeBackendApi, collectorId: Int = 100) =
        CollectorDetailViewModel(
            SavedStateHandle(mapOf(CollectorDetailViewModel.ARG_COLLECTOR_ID to collectorId)),
            CollectorRepository(CollectorServiceAdapter(api, dispatcher), dispatcher)
        )

    @Test
    fun `empieza en Loading mientras llega el detalle`() = runTest(dispatcher) {
        val viewModel = viewModelWith(FakeBackendApi(collectors = listOf(collectorDto(id = 100))))

        assertEquals(CollectorDetailUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `pasa de Loading a Success con el coleccionista y su coleccion`() = runTest(dispatcher) {
        val api = FakeBackendApi(
            collectors = listOf(collectorDto(id = 100, name = "Manolo Bellon")),
            collectorAlbums = listOf(collectorAlbumDto())
        )
        val viewModel = viewModelWith(api)
        assertEquals(CollectorDetailUiState.Loading, viewModel.uiState.value)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CollectorDetailUiState.Success)
        val collector = (state as CollectorDetailUiState.Success).collector
        assertEquals("Manolo Bellon", collector.name)
        assertEquals(1, collector.albums.size)
    }

    @Test
    fun `un coleccionista que no existe da Error con un mensaje entendible`() = runTest(dispatcher) {
        val viewModel = viewModelWith(FakeBackendApi(), collectorId = 999)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CollectorDetailUiState.Error)
        assertTrue((state as CollectorDetailUiState.Error).message.contains("No encontramos"))
    }

    @Test
    fun `sin conexion da Error`() = runTest(dispatcher) {
        val viewModel = viewModelWith(FakeBackendApi(error = IOException("sin red")))

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CollectorDetailUiState.Error)
        assertTrue((state as CollectorDetailUiState.Error).message.contains("conexión"))
    }

    @Test
    fun `reintentar vuelve a Loading y luego a Success cuando regresa la red`() = runTest(dispatcher) {
        val api = FakeBackendApi(
            collectors = listOf(collectorDto(id = 100)),
            error = IOException("sin red")
        )
        val viewModel = viewModelWith(api)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is CollectorDetailUiState.Error)

        api.error = null
        viewModel.loadCollector()
        assertEquals(CollectorDetailUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is CollectorDetailUiState.Success)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `sin el argumento obligatorio no se puede crear`() {
        CollectorDetailViewModel(
            SavedStateHandle(),
            CollectorRepository(CollectorServiceAdapter(FakeBackendApi(), dispatcher), dispatcher)
        )
    }
}
