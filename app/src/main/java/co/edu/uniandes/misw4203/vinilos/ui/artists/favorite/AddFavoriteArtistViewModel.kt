package co.edu.uniandes.misw4203.vinilos.ui.artists.favorite

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import co.edu.uniandes.misw4203.vinilos.VinilosApplication
import co.edu.uniandes.misw4203.vinilos.data.model.ArtistType
import co.edu.uniandes.misw4203.vinilos.data.model.FavoriteResult
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
 * ViewModel de la pantalla "Agregar a favoritos" (HU10). El artista llega completo en los
 * argumentos de navegación (id, tipo, nombre y cantidad de álbumes, copiados en
 * [SavedStateHandle]); solo se piden los coleccionistas del campo desplegable.
 *
 * A diferencia de HU11, que recibe solo el id del álbum y lo vuelve a cargar para pintar la
 * portada, los intérpretes y los tracks, la tarjeta del artista usa tres datos simples que el
 * detalle ya tiene. Así no hay una segunda carga que pueda fallar y el nombre para el mensaje
 * de éxito siempre está disponible.
 *
 * Lo que el usuario elige se guarda en [SavedStateHandle] para sobrevivir a la rotación.
 */
class AddFavoriteArtistViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val collectorRepository: CollectorRepository
) : ViewModel() {

    private val artistId: Int = requireNotNull(savedStateHandle.get<Int>(ARG_ARTIST_ID)) {
        "AddFavoriteArtistViewModel necesita el argumento '$ARG_ARTIST_ID'"
    }

    private val artistType: ArtistType = savedStateHandle.get<String>(ARG_ARTIST_TYPE)
        ?.let { name -> ArtistType.entries.firstOrNull { it.name == name } }
        ?: ArtistType.MUSICIAN

    private val artistName: String = savedStateHandle[ARG_ARTIST_NAME] ?: ""

    private val _uiState = MutableStateFlow(
        AddFavoriteArtistUiState(
            artistName = artistName,
            artistType = artistType,
            albumCount = savedStateHandle[ARG_ALBUM_COUNT] ?: 0,
            selectedCollectorId = savedStateHandle[KEY_COLLECTOR]
        )
    )
    val uiState: StateFlow<AddFavoriteArtistUiState> = _uiState.asStateFlow()

    private val _events = Channel<AddFavoriteArtistEvent>(Channel.BUFFERED)

    /** Avisos de una sola vez (favorito agregado o error al guardar). */
    val events: Flow<AddFavoriteArtistEvent> = _events.receiveAsFlow()

    init {
        loadCollectors()
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
                errors = it.errors - FavoriteFormField.COLLECTOR
            )
        }
    }

    /**
     * "Agregar a favoritos": si falta el coleccionista muestra el error y no envía nada;
     * si es válido guarda y avisa el resultado por [events] (o por el error del campo
     * Coleccionista si el artista ya era favorito suyo). Ignora toques mientras guarda.
     */
    fun save() {
        val current = _uiState.value
        if (current.isSaving) return

        val errors = FavoriteFormValidator.validate(current.selectedCollectorId)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors, duplicateCollectorName = null) }
            return
        }

        // El validador ya comprobó que hay coleccionista
        val collectorId = requireNotNull(current.selectedCollectorId)
        val collectorName = current.collectors.firstOrNull { it.id == collectorId }?.name.orEmpty()

        _uiState.update { it.copy(errors = emptyMap(), duplicateCollectorName = null, isSaving = true) }
        viewModelScope.launch {
            collectorRepository.addFavoriteArtist(collectorId, artistId, artistType).fold(
                onSuccess = { result ->
                    when (result) {
                        FavoriteResult.ADDED ->
                            // Sigue bloqueado: la pantalla se cierra al recibir el evento
                            _events.send(AddFavoriteArtistEvent.Added(artistName, collectorName))
                        FavoriteResult.ALREADY_FAVORITE ->
                            _uiState.update {
                                it.copy(isSaving = false, duplicateCollectorName = collectorName)
                            }
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(AddFavoriteArtistEvent.Failed(error.message.orEmpty()))
                }
            )
        }
    }

    companion object {
        /** Nombres de los argumentos en nav_graph.xml; el detalle del artista los arma. */
        const val ARG_ARTIST_ID = "artistId"
        const val ARG_ARTIST_TYPE = "artistType"
        const val ARG_ARTIST_NAME = "artistName"
        const val ARG_ALBUM_COUNT = "albumCount"

        private const val KEY_COLLECTOR = "add_favorite_collector"

        /** Crea el ViewModel con los argumentos de navegación y el repositorio de AppContainer. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as VinilosApplication
                AddFavoriteArtistViewModel(createSavedStateHandle(), app.appContainer.collectorRepository)
            }
        }
    }
}
