package co.edu.uniandes.misw4203.vinilos.ui.albums.tracks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import co.edu.uniandes.misw4203.vinilos.VinilosApplication
import co.edu.uniandes.misw4203.vinilos.data.model.NewTrack
import co.edu.uniandes.misw4203.vinilos.data.repository.AlbumRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel de la pantalla "Agregar track" (HU08). El id del álbum llega en los
 * argumentos de navegación (copiados en [SavedStateHandle]). Carga el álbum para la
 * tarjeta superior, valida el formulario y le pide al repositorio guardar la canción.
 */
class AddTrackViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val albumRepository: AlbumRepository
) : ViewModel() {

    private val albumId: Int = requireNotNull(savedStateHandle.get<Int>(ARG_ALBUM_ID)) {
        "AddTrackViewModel necesita el argumento '$ARG_ALBUM_ID'"
    }

    private var name: String = savedStateHandle[KEY_NAME] ?: ""
    private var duration: String = savedStateHandle[KEY_DURATION] ?: ""

    private val _uiState = MutableStateFlow(AddTrackUiState())
    val uiState: StateFlow<AddTrackUiState> = _uiState.asStateFlow()

    private val _events = Channel<AddTrackEvent>(Channel.BUFFERED)

    /** Avisos de una sola vez (canción agregada o error al guardar). */
    val events: Flow<AddTrackEvent> = _events.receiveAsFlow()

    init {
        loadAlbum()
    }

    /**
     * Pide el álbum para la tarjeta superior. Si falla, la tarjeta no se muestra
     * pero el formulario sigue funcionando.
     */
    private fun loadAlbum() {
        viewModelScope.launch {
            albumRepository.getAlbum(albumId).onSuccess { album ->
                _uiState.update { it.copy(album = album) }
            }
        }
    }

    fun onNameChanged(value: String) {
        if (value == name) return
        name = value
        savedStateHandle[KEY_NAME] = value
        clearError(TrackFormField.NAME)
    }

    fun onDurationChanged(value: String) {
        if (value == duration) return
        duration = value
        savedStateHandle[KEY_DURATION] = value
        clearError(TrackFormField.DURATION)
    }

    /**
     * "Guardar track": si el formulario tiene errores los muestra y no envía nada;
     * si es válido guarda la canción y avisa el resultado por [events].
     */
    fun save() {
        if (_uiState.value.isSaving) return

        val errors = TrackFormValidator.validate(name, duration)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }

        // El validador ya comprobó que la duración tiene el formato correcto
        val track = NewTrack(
            name = name.trim(),
            duration = requireNotNull(TrackFormValidator.normalizeDuration(duration))
        )

        _uiState.update { it.copy(errors = emptyMap(), isSaving = true) }
        viewModelScope.launch {
            val result = albumRepository.addTrack(albumId, track)
            _uiState.update { it.copy(isSaving = false) }
            result.fold(
                onSuccess = { saved ->
                    _events.send(AddTrackEvent.Added(saved.id, saved.name.ifBlank { track.name }))
                },
                onFailure = { error ->
                    _events.send(AddTrackEvent.Failed(error.message.orEmpty()))
                }
            )
        }
    }

    private fun clearError(field: TrackFormField) {
        if (field in _uiState.value.errors) {
            _uiState.update { it.copy(errors = it.errors - field) }
        }
    }

    companion object {
        /** Nombre del argumento en nav_graph.xml (el mismo que usa el detalle del álbum). */
        const val ARG_ALBUM_ID = "albumId"

        private const val KEY_NAME = "add_track_name"
        private const val KEY_DURATION = "add_track_duration"

        /** Crea el ViewModel con los argumentos de navegación y el repositorio de AppContainer. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as VinilosApplication
                AddTrackViewModel(createSavedStateHandle(), app.appContainer.albumRepository)
            }
        }
    }
}
