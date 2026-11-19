package co.edu.uniandes.misw4203.vinilos.ui.albums.collection

import androidx.lifecycle.SavedStateHandle
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.albumDto
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.collectorAlbumDto
import co.edu.uniandes.misw4203.vinilos.data.FakeBackendApi.Companion.collectorDto
import co.edu.uniandes.misw4203.vinilos.data.adapter.AlbumServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.adapter.CollectorServiceAdapter
import co.edu.uniandes.misw4203.vinilos.data.model.CollectorAlbumStatus
import co.edu.uniandes.misw4203.vinilos.data.repository.AlbumRepository
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

/** HU11: ViewModel de la pantalla "Agregar a colección". */
@OptIn(ExperimentalCoroutinesApi::class)
class AddToCollectionViewModelTest {

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

    private fun apiWith(inCollection: Boolean = false) = FakeBackendApi(
        albums = listOf(albumDto(id = 100)),
        collectors = listOf(collectorDto(id = 100, name = "Manolo Bellon"), collectorDto(id = 101, name = "Jaime Monsalve")),
        collectorAlbums = if (inCollection) listOf(collectorAlbumDto(album = albumDto(id = 100))) else emptyList()
    )

    private fun viewModelWith(
        api: FakeBackendApi = apiWith(),
        savedStateHandle: SavedStateHandle =
            SavedStateHandle(mapOf(AddToCollectionViewModel.ARG_ALBUM_ID to 100))
    ) = AddToCollectionViewModel(
        savedStateHandle,
        AlbumRepository(AlbumServiceAdapter(api, dispatcher), dispatcher),
        CollectorRepository(CollectorServiceAdapter(api, dispatcher), dispatcher)
    )

    private fun AddToCollectionViewModel.fillValidForm(collectorId: Int = 100) {
        onCollectorSelected(collectorId)
        onPriceChanged("35000")
        onStatusSelected(CollectorAlbumStatus.ACTIVE)
    }

    @Test
    fun `carga el album y los coleccionistas`() = runTest(dispatcher) {
        val viewModel = viewModelWith()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Buscando América", state.album?.name)
        assertEquals(listOf(100, 101), state.collectors.map { it.id })
        assertFalse(state.isLoadingCollectors)
        assertFalse(state.collectorsLoadFailed)
    }

    @Test
    fun `guardar vacio muestra los tres errores y el aviso sin llamar al backend`() = runTest(dispatcher) {
        val api = apiWith()
        val viewModel = viewModelWith(api)
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(
            mapOf(
                CollectionFormField.COLLECTOR to CollectionFormError.REQUIRED,
                CollectionFormField.PRICE to CollectionFormError.REQUIRED,
                CollectionFormField.STATUS to CollectionFormError.REQUIRED
            ),
            state.errors
        )
        assertTrue(state.showErrorBanner)
        assertTrue(api.addedToCollection.isEmpty())
    }

    @Test
    fun `un precio cero muestra el error de precio y no envia`() = runTest(dispatcher) {
        val api = apiWith()
        val viewModel = viewModelWith(api)
        advanceUntilIdle()
        viewModel.fillValidForm()
        viewModel.onPriceChanged("0")

        viewModel.save()
        advanceUntilIdle()

        assertEquals(
            mapOf(CollectionFormField.PRICE to CollectionFormError.INVALID_PRICE),
            viewModel.uiState.value.errors
        )
        assertTrue(api.addedToCollection.isEmpty())
    }

    @Test
    fun `corregir un campo quita su error`() = runTest(dispatcher) {
        val viewModel = viewModelWith()
        advanceUntilIdle()
        viewModel.save()

        viewModel.onCollectorSelected(100)
        viewModel.onPriceChanged("500")
        viewModel.onStatusSelected(CollectorAlbumStatus.INACTIVE)

        val state = viewModel.uiState.value
        assertTrue(state.errors.isEmpty())
        assertFalse(state.showErrorBanner)
    }

    @Test
    fun `si el album ya esta en la coleccion muestra el error y no emite Added`() = runTest(dispatcher) {
        val api = apiWith(inCollection = true)
        val viewModel = viewModelWith(api)
        advanceUntilIdle()
        viewModel.fillValidForm()

        viewModel.save()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Manolo Bellon", state.duplicateCollectorName)
        assertFalse(state.isSaving)
        assertTrue(api.addedToCollection.isEmpty())
    }

    @Test
    fun `al cambiar de coleccionista se quita el error de duplicado`() = runTest(dispatcher) {
        val viewModel = viewModelWith(apiWith(inCollection = true))
        advanceUntilIdle()
        viewModel.fillValidForm()
        viewModel.save()
        advanceUntilIdle()

        viewModel.onCollectorSelected(101)

        assertNull(viewModel.uiState.value.duplicateCollectorName)
    }

    @Test
    fun `al guardar con exito emite Added con los nombres y envia precio y estado`() = runTest(dispatcher) {
        val api = apiWith()
        val viewModel = viewModelWith(api)
        advanceUntilIdle()
        viewModel.fillValidForm(collectorId = 101)

        viewModel.save()
        advanceUntilIdle()

        assertEquals(
            AddToCollectionEvent.Added("Buscando América", "Jaime Monsalve"),
            viewModel.events.first()
        )
        val (collectorId, albumId, body) = api.addedToCollection.single()
        assertEquals(101, collectorId)
        assertEquals(100, albumId)
        assertEquals(35000, body.price)
        assertEquals("Active", body.status)
    }

    @Test
    fun `un fallo emite Failed y conserva los datos`() = runTest(dispatcher) {
        val api = apiWith()
        val viewModel = viewModelWith(api)
        advanceUntilIdle()
        viewModel.fillValidForm()
        api.error = IOException("sin red")

        viewModel.save()
        advanceUntilIdle()

        val event = viewModel.events.first()
        assertTrue(event is AddToCollectionEvent.Failed)
        assertTrue((event as AddToCollectionEvent.Failed).message.contains("conexión"))
        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertEquals(100, state.selectedCollectorId)
        assertEquals(CollectorAlbumStatus.ACTIVE, state.status)
    }

    @Test
    fun `tocar guardar dos veces seguidas envia una sola vez`() = runTest(dispatcher) {
        val api = apiWith()
        val viewModel = viewModelWith(api)
        advanceUntilIdle()
        viewModel.fillValidForm()

        viewModel.save()
        assertTrue(viewModel.uiState.value.isSaving)
        viewModel.save()
        advanceUntilIdle()

        assertEquals(1, api.addedToCollection.size)
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
    fun `restaura lo elegido desde SavedStateHandle`() = runTest(dispatcher) {
        val saved = SavedStateHandle(mapOf(AddToCollectionViewModel.ARG_ALBUM_ID to 100))
        val first = viewModelWith(savedStateHandle = saved)
        advanceUntilIdle()
        first.fillValidForm(collectorId = 101)
        first.onStatusSelected(CollectorAlbumStatus.INACTIVE)

        // Un ViewModel nuevo con el mismo SavedStateHandle (rotación o muerte del proceso)
        val api = apiWith()
        val second = viewModelWith(api, saved)
        advanceUntilIdle()

        val state = second.uiState.value
        assertEquals(101, state.selectedCollectorId)
        assertEquals(CollectorAlbumStatus.INACTIVE, state.status)

        // El precio también se conserva: el formulario ya es válido sin volver a escribirlo
        second.save()
        advanceUntilIdle()
        val (collectorId, _, body) = api.addedToCollection.single()
        assertEquals(101, collectorId)
        assertEquals(35000, body.price)
    }
}
