package co.edu.uniandes.misw4203.vinilos.ui.artists.favorite

import androidx.lifecycle.SavedStateHandle
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.bandDto
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.collectorDto
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.musicianDto
import co.edu.uniandes.misw4203.vinilos.data.adapter.CollectorServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.dto.PerformerDto
import co.edu.uniandes.misw4203.vinilos.data.model.ArtistType
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/** HU10: ViewModel de la pantalla "Agregar a favoritos". */
@OptIn(ExperimentalCoroutinesApi::class)
class AddFavoriteArtistViewModelTest {

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

    private fun apiWith(alreadyFavorite: Boolean = false) = FakeBackendApi(
        collectors = listOf(
            collectorDto(
                id = 100,
                name = "Manolo Bellon",
                favoritePerformers = if (alreadyFavorite) listOf(PerformerDto(100, "Rubén Blades", "rb.jpg")) else null
            ),
            collectorDto(id = 101, name = "Jaime Monsalve")
        ),
        musicians = listOf(musicianDto(id = 100)),
        bands = listOf(bandDto(id = 101))
    )

    private fun args(type: ArtistType = ArtistType.MUSICIAN, id: Int = 100, name: String = "Rubén Blades") =
        mapOf(
            AddFavoriteArtistViewModel.ARG_ARTIST_ID to id,
            AddFavoriteArtistViewModel.ARG_ARTIST_TYPE to type.name,
            AddFavoriteArtistViewModel.ARG_ARTIST_NAME to name,
            AddFavoriteArtistViewModel.ARG_ALBUM_COUNT to 3
        )

    private fun viewModelWith(
        api: FakeBackendApi = apiWith(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(args())
    ) = AddFavoriteArtistViewModel(
        savedStateHandle,
        CollectorRepository(CollectorServiceAdapter(api, dispatcher), dispatcher)
    )

    @Test
    fun `la tarjeta usa los argumentos y carga los coleccionistas`() = runTest(dispatcher) {
        val api = apiWith()
        val viewModel = viewModelWith(api)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Rubén Blades", state.artistName)
        assertEquals(ArtistType.MUSICIAN, state.artistType)
        assertEquals(3, state.albumCount)
        assertEquals(listOf(100, 101), state.collectors.map { it.id })
        assertFalse(state.isLoadingCollectors)
        assertFalse(state.collectorsLoadFailed)
        // No pide el artista: solo existe la carga de coleccionistas
        assertEquals(1, api.listRequests)
    }

    @Test
    fun `un tipo desconocido se toma como musico`() = runTest(dispatcher) {
        val handle = SavedStateHandle(args() + (AddFavoriteArtistViewModel.ARG_ARTIST_TYPE to "OTRO"))

        assertEquals(ArtistType.MUSICIAN, viewModelWith(savedStateHandle = handle).uiState.value.artistType)
    }

    @Test
    fun `guardar sin coleccionista muestra el error y el aviso sin llamar al backend`() = runTest(dispatcher) {
        val api = apiWith()
        val viewModel = viewModelWith(api)
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(mapOf(FavoriteFormField.COLLECTOR to FavoriteFormError.REQUIRED), state.errors)
        assertTrue(state.showErrorBanner)
        assertTrue(api.addedFavorites.isEmpty())
    }

    @Test
    fun `elegir coleccionista quita el error`() = runTest(dispatcher) {
        val viewModel = viewModelWith()
        advanceUntilIdle()
        viewModel.save()

        viewModel.onCollectorSelected(100)

        val state = viewModel.uiState.value
        assertTrue(state.errors.isEmpty())
        assertFalse(state.showErrorBanner)
        assertEquals(100, state.selectedCollectorId)
    }

    @Test
    fun `si el artista ya es favorito se avisa en el campo y no se envia`() = runTest(dispatcher) {
        val api = apiWith(alreadyFavorite = true)
        val viewModel = viewModelWith(api)
        advanceUntilIdle()
        viewModel.onCollectorSelected(100)

        viewModel.save()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Manolo Bellon", state.duplicateCollectorName)
        assertFalse(state.isSaving)
        assertTrue(api.addedFavorites.isEmpty())
    }

    @Test
    fun `al cambiar de coleccionista se quita el error de duplicado`() = runTest(dispatcher) {
        val viewModel = viewModelWith(apiWith(alreadyFavorite = true))
        advanceUntilIdle()
        viewModel.onCollectorSelected(100)
        viewModel.save()
        advanceUntilIdle()

        viewModel.onCollectorSelected(101)

        assertNull(viewModel.uiState.value.duplicateCollectorName)
    }

    @Test
    fun `al guardar con exito emite Added con los nombres y usa la ruta de musicos`() = runTest(dispatcher) {
        val api = apiWith()
        val viewModel = viewModelWith(api)
        advanceUntilIdle()
        viewModel.onCollectorSelected(101)

        viewModel.save()
        advanceUntilIdle()

        assertEquals(
            AddFavoriteArtistEvent.Added("Rubén Blades", "Jaime Monsalve"),
            viewModel.events.first()
        )
        assertEquals(listOf(Triple(101, 100, ArtistType.MUSICIAN)), api.addedFavorites)
    }

    @Test
    fun `una banda se envia por la ruta de bandas`() = runTest(dispatcher) {
        val api = apiWith()
        val viewModel = viewModelWith(api, SavedStateHandle(args(ArtistType.BAND, id = 101, name = "Queen")))
        advanceUntilIdle()
        viewModel.onCollectorSelected(100)

        viewModel.save()
        advanceUntilIdle()

        assertEquals(listOf(Triple(100, 101, ArtistType.BAND)), api.addedFavorites)
    }

    @Test
    fun `un fallo de red emite Failed y conserva la seleccion`() = runTest(dispatcher) {
        val api = apiWith()
        val viewModel = viewModelWith(api)
        advanceUntilIdle()
        viewModel.onCollectorSelected(100)
        api.error = IOException("sin red")

        viewModel.save()
        advanceUntilIdle()

        val event = viewModel.events.first()
        assertTrue(event is AddFavoriteArtistEvent.Failed)
        assertTrue((event as AddFavoriteArtistEvent.Failed).message.contains("conexión"))
        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertEquals(100, state.selectedCollectorId)
    }

    @Test
    fun `tocar guardar dos veces seguidas envia una sola vez`() = runTest(dispatcher) {
        val api = apiWith()
        val viewModel = viewModelWith(api)
        advanceUntilIdle()
        viewModel.onCollectorSelected(100)

        viewModel.save()
        assertTrue(viewModel.uiState.value.isSaving)
        viewModel.save()
        advanceUntilIdle()

        assertEquals(1, api.addedFavorites.size)
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
    fun `restaura el coleccionista elegido desde SavedStateHandle`() = runTest(dispatcher) {
        val saved = SavedStateHandle(args())
        val first = viewModelWith(savedStateHandle = saved)
        advanceUntilIdle()
        first.onCollectorSelected(101)

        // Un ViewModel nuevo con el mismo SavedStateHandle (rotación o muerte del proceso)
        val api = apiWith()
        val second = viewModelWith(api, saved)
        advanceUntilIdle()

        assertEquals(101, second.uiState.value.selectedCollectorId)
        second.save()
        advanceUntilIdle()
        assertEquals(listOf(Triple(101, 100, ArtistType.MUSICIAN)), api.addedFavorites)
    }
}
