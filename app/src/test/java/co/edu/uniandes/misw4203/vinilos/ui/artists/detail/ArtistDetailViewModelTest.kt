package co.edu.uniandes.misw4203.vinilos.ui.artists.detail

import androidx.lifecycle.SavedStateHandle
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.bandDto
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.musicianDto
import co.edu.uniandes.misw4203.vinilos.data.adapter.ArtistServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.model.ArtistType
import co.edu.uniandes.misw4203.vinilos.data.repository.ArtistRepository
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
class ArtistDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModelWith(
        api: FakeBackendApi,
        artistId: Int = 100,
        type: ArtistType = ArtistType.MUSICIAN
    ) = ArtistDetailViewModel(
        SavedStateHandle(
            mapOf(
                ArtistDetailViewModel.ARG_ARTIST_ID to artistId,
                ArtistDetailViewModel.ARG_ARTIST_TYPE to type.name
            )
        ),
        ArtistRepository(ArtistServiceAdapter(api, dispatcher), dispatcher)
    )

    @Test
    fun `empieza en Loading mientras llega el detalle del artista`() = runTest(dispatcher) {
        val viewModel = viewModelWith(FakeBackendApi(musicians = listOf(musicianDto(id = 100))))

        assertEquals(ArtistDetailUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `muestra el detalle de un musico correctamente`() = runTest(dispatcher) {
        val api = FakeBackendApi(musicians = listOf(musicianDto(id = 100, name = "Rubén Blades")))
        val viewModel = viewModelWith(api, artistId = 100, type = ArtistType.MUSICIAN)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ArtistDetailUiState.Success)
        assertEquals("Rubén Blades", (state as ArtistDetailUiState.Success).artistDetail.name)
        assertEquals(ArtistType.MUSICIAN, state.artistDetail.type)
    }

    @Test
    fun `muestra el detalle de una banda correctamente`() = runTest(dispatcher) {
        val api = FakeBackendApi(bands = listOf(bandDto(id = 101, name = "Queen")))
        val viewModel = viewModelWith(api, artistId = 101, type = ArtistType.BAND)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ArtistDetailUiState.Success)
        assertEquals("Queen", (state as ArtistDetailUiState.Success).artistDetail.name)
        assertEquals(ArtistType.BAND, state.artistDetail.type)
    }

    @Test
    fun `un artista que no existe da Error`() = runTest(dispatcher) {
        val viewModel = viewModelWith(FakeBackendApi(musicians = emptyList()), artistId = 999)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ArtistDetailUiState.Error)
    }

    @Test
    fun `sin conexion da Error y reintentar vuelve a pedir el artista`() = runTest(dispatcher) {
        val viewModel = viewModelWith(FakeBackendApi(error = IOException("sin red")))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is ArtistDetailUiState.Error)

        viewModel.loadArtistDetail()

        assertEquals(ArtistDetailUiState.Loading, viewModel.uiState.value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `sin los argumentos obligatorios no se puede crear`() {
        ArtistDetailViewModel(SavedStateHandle(), ArtistRepository(ArtistServiceAdapter(FakeBackendApi(), dispatcher), dispatcher))
    }
}
