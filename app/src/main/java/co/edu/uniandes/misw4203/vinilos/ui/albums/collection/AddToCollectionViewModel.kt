package co.edu.uniandes.misw4203.vinilos.ui.albums.collection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import co.edu.uniandes.misw4203.vinilos.VinilosApplication
import co.edu.uniandes.misw4203.vinilos.data.model.CollectionResult
import co.edu.uniandes.misw4203.vinilos.data.model.CollectorAlbumStatus
import co.edu.uniandes.misw4203.vinilos.data.repository.AlbumRepository
import co.edu.uniandes.misw4203.vinilos.data.repository.CollectorRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel de la pantalla "Agregar a colección" (HU11). El id del álbum llega en los
 * argumentos de navegación (copiados en [SavedStateHandle]). Carga en paralelo el álbum
 * (tarjeta) y los coleccionistas (campo desplegable), valida el formulario y le pide al
 * repositorio guardar. Lo que el usuario elige se guarda en [SavedStateHandle].
 */
class AddToCollectionViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val albumRepository: AlbumRepository,
    private val collectorRepository: CollectorRepository
) : ViewModel() {

    private val albumId: Int = requireNotNull(savedStateHandle.get<Int>(ARG_ALBUM_ID)) {
        "AddToCollectionViewModel necesita el argumento '$ARG_ALBUM_ID'"
    }

    private var price: String = savedStateHandle[KEY_PRICE] ?: ""

    private val _uiState = MutableStateFlow(
        AddToCollectionUiState(
            selectedCollectorId = savedStateHandle[KEY_COLLECTOR],
            status = savedStateHandle.get<String>(KEY_STATUS)
                ?.let { name -> CollectorAlbumStatus.entries.firstOrNull { it.name == name } }
        )
    )
    val uiState: StateFlow<AddToCollectionUiState> = _uiState.asStateFlow()

    private val _events = Channel<AddToCollectionEvent>(Channel.BUFFERED)

    /** Avisos de una sola vez (álbum agregado o error al guardar). */
    val events: Flow<AddToCollectionEvent> = _events.receiveAsFlow()

    init {
        loadAlbum()
        loadCollectors()
    }

    /** Pide el álbum de la tarjeta. Si falla, no se muestra la tarjeta pero el formulario sigue. */
    private fun loadAlbum() {
        viewModelScope.launch {
            albumRepository.getAlbum(albumId).onSuccess { album ->
                _uiState.update { it.copy(album = album) }
            }
        }
    }

    private fun loadCollectors() {
        viewModelScope.launch {
            collectorRepository.getCollectors().fold(
                onSuccess = { list ->
                    _uiState.update {
                        it.copy(collectors = list, isLoadingCollectors = false, collectorsLoadFailed = false)
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoadingCollectors = false, collectorsLoadFailed = true) }
                }
            )
        }
    }

    /** Vuelve a pedir los coleccionistas después de un fallo. */
    fun retryCollectors() {
        if (_uiState.value.isLoadingCollectors) return
        _uiState.update { it.copy(isLoadingCollectors = true, collectorsLoadFailed = false) }
        loadCollectors()
    }

    fun onCollectorSelected(id: Int) {
        savedStateHandle[KEY_COLLECTOR] = id
        _uiState.update {
            it.copy(
                selectedCollectorId = id,
                duplicateCollectorName = null,
                errors = it.errors - CollectionFormField.COLLECTOR
            )
        }
    }

    fun onPriceChanged(text: String) {
        if (text == price) return
        price = text
        savedStateHandle[KEY_PRICE] = text
        _uiState.update { it.copy(errors = it.errors - CollectionFormField.PRICE) }
    }

    fun onStatusSelected(status: CollectorAlbumStatus) {
        savedStateHandle[KEY_STATUS] = status.name
        _uiState.update { it.copy(status = status, errors = it.errors - CollectionFormField.STATUS) }
    }

    /**
     * "Agregar a la colección": si el formulario tiene errores los muestra y no envía nada;
     * si es válido guarda y avisa el resultado por [events] (o por el error del campo
     * Coleccionista si el álbum ya estaba en su colección). Ignora toques mientras guarda.
     */
    fun save() {
        val current = _uiState.value
        if (current.isSaving) return

        val errors = CollectionFormValidator.validate(current.selectedCollectorId, price, current.status)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors, duplicateCollectorName = null) }
            return
        }

        // El validador ya comprobó que hay coleccionista, estado y un precio válido
        val collectorId = requireNotNull(current.selectedCollectorId)
        val status = requireNotNull(current.status)
        val priceValue = requireNotNull(CollectionFormValidator.parsePrice(price))
        val collectorName = current.collectors.firstOrNull { it.id == collectorId }?.name.orEmpty()

        _uiState.update { it.copy(errors = emptyMap(), duplicateCollectorName = null, isSaving = true) }
        viewModelScope.launch {
            collectorRepository.addAlbumToCollector(collectorId, albumId, priceValue, status).fold(
                onSuccess = { result ->
                    when (result) {
                        CollectionResult.ADDED -> {
                            // Sigue bloqueado: la pantalla se cierra al recibir el evento
                            val albumName = _uiState.value.album?.name.orEmpty()
                            _events.send(AddToCollectionEvent.Added(albumName, collectorName))
                        }
                        CollectionResult.ALREADY_IN_COLLECTION ->
                            _uiState.update {
                                it.copy(isSaving = false, duplicateCollectorName = collectorName)
                            }
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(AddToCollectionEvent.Failed(error.message.orEmpty()))
                }
            )
        }
    }

    companion object {
        /** Nombre del argumento en nav_graph.xml (el mismo que usa el detalle del álbum). */
        const val ARG_ALBUM_ID = "albumId"

        private const val KEY_COLLECTOR = "add_to_collection_collector"
        private const val KEY_PRICE = "add_to_collection_price"
        private const val KEY_STATUS = "add_to_collection_status"

        /** Crea el ViewModel con los argumentos de navegación y los repositorios de AppContainer. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as VinilosApplication
                AddToCollectionViewModel(
                    createSavedStateHandle(),
                    app.appContainer.albumRepository,
                    app.appContainer.collectorRepository
                )
            }
        }
    }
}
