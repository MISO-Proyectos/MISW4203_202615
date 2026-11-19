package co.edu.uniandes.misw4203.vinilos.ui.albums.comments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import co.edu.uniandes.misw4203.vinilos.VinilosApplication
import co.edu.uniandes.misw4203.vinilos.data.model.NewComment
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
 * ViewModel de la pantalla "Comentar álbum" (HU09). El id del álbum llega en los
 * argumentos de navegación (copiados en [SavedStateHandle]). Carga el álbum (tarjeta) y
 * los coleccionistas (campo desplegable), valida el formulario y le pide al repositorio
 * publicar el comentario. Lo que el usuario escribe o elige se guarda en [SavedStateHandle].
 */
class AddCommentViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val albumRepository: AlbumRepository,
    private val collectorRepository: CollectorRepository
) : ViewModel() {

    private val albumId: Int = requireNotNull(savedStateHandle.get<Int>(ARG_ALBUM_ID)) {
        "AddCommentViewModel necesita el argumento '$ARG_ALBUM_ID'"
    }

    private var description: String = savedStateHandle[KEY_DESCRIPTION] ?: ""

    private val _uiState = MutableStateFlow(
        AddCommentUiState(
            selectedCollectorId = savedStateHandle[KEY_COLLECTOR],
            rating = savedStateHandle[KEY_RATING] ?: 0
        )
    )
    val uiState: StateFlow<AddCommentUiState> = _uiState.asStateFlow()

    private val _events = Channel<AddCommentEvent>(Channel.BUFFERED)

    /** Avisos de una sola vez (comentario publicado o error al publicar). */
    val events: Flow<AddCommentEvent> = _events.receiveAsFlow()

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
            it.copy(selectedCollectorId = id, errors = it.errors - CommentFormField.COLLECTOR)
        }
    }

    fun onRatingChanged(value: Int) {
        if (value == _uiState.value.rating) return
        savedStateHandle[KEY_RATING] = value
        _uiState.update { it.copy(rating = value, errors = it.errors - CommentFormField.RATING) }
    }

    fun onDescriptionChanged(value: String) {
        if (value == description) return
        description = value
        savedStateHandle[KEY_DESCRIPTION] = value
        _uiState.update { it.copy(errors = it.errors - CommentFormField.DESCRIPTION) }
    }

    /**
     * "Publicar comentario": si el formulario tiene errores los muestra y no envía nada;
     * si es válido publica y avisa el resultado por [events]. Ignora toques mientras envía.
     */
    fun save() {
        val current = _uiState.value
        if (current.isSaving) return

        val errors = CommentFormValidator.validate(current.selectedCollectorId, current.rating, description)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }

        // El validador ya comprobó que hay coleccionista, calificación y texto
        val comment = NewComment(
            collectorId = requireNotNull(current.selectedCollectorId),
            rating = current.rating,
            description = description.trim()
        )

        _uiState.update { it.copy(errors = emptyMap(), isSaving = true) }
        viewModelScope.launch {
            albumRepository.addComment(albumId, comment).fold(
                onSuccess = {
                    // Sigue bloqueado: la pantalla se cierra al recibir el evento
                    _events.send(AddCommentEvent.Published)
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(AddCommentEvent.Failed(error.message.orEmpty()))
                }
            )
        }
    }

    companion object {
        /** Nombre del argumento en nav_graph.xml (el mismo que usa el detalle del álbum). */
        const val ARG_ALBUM_ID = "albumId"

        private const val KEY_COLLECTOR = "add_comment_collector"
        private const val KEY_RATING = "add_comment_rating"
        private const val KEY_DESCRIPTION = "add_comment_description"

        /** Crea el ViewModel con los argumentos de navegación y los repositorios de AppContainer. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as VinilosApplication
                AddCommentViewModel(
                    createSavedStateHandle(),
                    app.appContainer.albumRepository,
                    app.appContainer.collectorRepository
                )
            }
        }
    }
}
