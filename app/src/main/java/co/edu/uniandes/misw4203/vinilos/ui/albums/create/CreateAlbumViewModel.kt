package co.edu.uniandes.misw4203.vinilos.ui.albums.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import co.edu.uniandes.misw4203.vinilos.VinilosApplication
import co.edu.uniandes.misw4203.vinilos.data.model.AlbumGenre
import co.edu.uniandes.misw4203.vinilos.data.model.NewAlbum
import co.edu.uniandes.misw4203.vinilos.data.model.RecordLabel
import co.edu.uniandes.misw4203.vinilos.data.model.ReleaseDate
import co.edu.uniandes.misw4203.vinilos.data.repository.AlbumRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * ViewModel de la pantalla "Agregar álbum" (HU07). Recibe lo que escribe o elige el
 * usuario, valida el formulario y le pide al repositorio crear el álbum.
 * Los valores se guardan en [SavedStateHandle] para no perderlos si Android cierra el proceso.
 * [today] se puede cambiar en las pruebas para fijar la fecha actual.
 */
class CreateAlbumViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val albumRepository: AlbumRepository,
    private val today: () -> ReleaseDate = ::currentDate
) : ViewModel() {

    private var name: String = savedStateHandle[KEY_NAME] ?: ""
    private var cover: String = savedStateHandle[KEY_COVER] ?: ""
    private var description: String = savedStateHandle[KEY_DESCRIPTION] ?: ""

    private val _uiState = MutableStateFlow(
        CreateAlbumUiState(
            releaseDate = restoreDate(),
            genre = savedStateHandle.get<String>(KEY_GENRE)
                ?.let { saved -> AlbumGenre.entries.firstOrNull { it.name == saved } },
            recordLabel = savedStateHandle.get<String>(KEY_RECORD_LABEL)
                ?.let { saved -> RecordLabel.entries.firstOrNull { it.name == saved } }
        )
    )
    val uiState: StateFlow<CreateAlbumUiState> = _uiState.asStateFlow()

    private val _events = Channel<CreateAlbumEvent>(Channel.BUFFERED)

    /** Avisos de una sola vez (álbum creado o error al guardar). */
    val events: Flow<CreateAlbumEvent> = _events.receiveAsFlow()

    fun onNameChanged(value: String) {
        if (value == name) return
        name = value
        savedStateHandle[KEY_NAME] = value
        clearError(AlbumFormField.NAME)
    }

    fun onCoverChanged(value: String) {
        if (value == cover) return
        cover = value
        savedStateHandle[KEY_COVER] = value
        clearError(AlbumFormField.COVER)
    }

    fun onDescriptionChanged(value: String) {
        if (value == description) return
        description = value
        savedStateHandle[KEY_DESCRIPTION] = value
        clearError(AlbumFormField.DESCRIPTION)
    }

    fun onReleaseDateSelected(date: ReleaseDate) {
        savedStateHandle[KEY_DATE] = intArrayOf(date.year, date.month ?: 1, date.day ?: 1)
        _uiState.update {
            it.copy(releaseDate = date, errors = it.errors - AlbumFormField.RELEASE_DATE)
        }
    }

    fun onGenreSelected(genre: AlbumGenre) {
        savedStateHandle[KEY_GENRE] = genre.name
        _uiState.update { it.copy(genre = genre, errors = it.errors - AlbumFormField.GENRE) }
    }

    fun onRecordLabelSelected(recordLabel: RecordLabel) {
        savedStateHandle[KEY_RECORD_LABEL] = recordLabel.name
        _uiState.update {
            it.copy(recordLabel = recordLabel, errors = it.errors - AlbumFormField.RECORD_LABEL)
        }
    }

    /**
     * "Guardar álbum": si el formulario tiene errores los muestra y no envía nada;
     * si es válido crea el álbum y avisa el resultado por [events].
     */
    fun save() {
        val state = _uiState.value
        if (state.isSaving) return

        val errors = AlbumFormValidator.validate(
            name = name,
            cover = cover,
            releaseDate = state.releaseDate,
            genre = state.genre,
            recordLabel = state.recordLabel,
            description = description,
            today = today()
        )
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }

        // El validador ya comprobó que estos valores no son null
        val album = NewAlbum(
            name = name.trim(),
            cover = cover.trim(),
            releaseDate = requireNotNull(state.releaseDate),
            genre = requireNotNull(state.genre),
            recordLabel = requireNotNull(state.recordLabel),
            description = description.trim()
        )

        _uiState.update { it.copy(errors = emptyMap(), isSaving = true) }
        viewModelScope.launch {
            val result = albumRepository.createAlbum(album)
            _uiState.update { it.copy(isSaving = false) }
            result.fold(
                onSuccess = { created ->
                    _events.send(CreateAlbumEvent.Created(created.name.ifBlank { album.name }))
                },
                onFailure = { error ->
                    _events.send(CreateAlbumEvent.Failed(error.message.orEmpty()))
                }
            )
        }
    }

    private fun clearError(field: AlbumFormField) {
        if (field in _uiState.value.errors) {
            _uiState.update { it.copy(errors = it.errors - field) }
        }
    }

    private fun restoreDate(): ReleaseDate? =
        savedStateHandle.get<IntArray>(KEY_DATE)
            ?.takeIf { it.size == 3 }
            ?.let { ReleaseDate(it[0], it[1], it[2]) }

    companion object {
        private const val KEY_NAME = "create_album_name"
        private const val KEY_COVER = "create_album_cover"
        private const val KEY_DESCRIPTION = "create_album_description"
        private const val KEY_DATE = "create_album_date"
        private const val KEY_GENRE = "create_album_genre"
        private const val KEY_RECORD_LABEL = "create_album_record_label"

        /** Fecha de hoy en el teléfono, sin java.time (no existe en API 24-25). */
        fun currentDate(): ReleaseDate {
            val calendar = Calendar.getInstance()
            return ReleaseDate(
                year = calendar.get(Calendar.YEAR),
                month = calendar.get(Calendar.MONTH) + 1,
                day = calendar.get(Calendar.DAY_OF_MONTH)
            )
        }

        /** Crea el ViewModel con el repositorio de AppContainer. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as VinilosApplication
                CreateAlbumViewModel(createSavedStateHandle(), app.appContainer.albumRepository)
            }
        }
    }
}
