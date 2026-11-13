package co.edu.uniandes.misw4203.vinilos.ui.collectors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import co.edu.uniandes.misw4203.vinilos.VinilosApplication
import co.edu.uniandes.misw4203.vinilos.data.repository.CollectorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel del listado de coleccionistas (HU05). Pide la lista al repositorio y
 * expone el estado de la pantalla. Como sobrevive a las rotaciones, los coleccionistas
 * no se vuelven a pedir al girar el teléfono.
 */
class CollectorListViewModel(
    private val collectorRepository: CollectorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CollectorListUiState>(CollectorListUiState.Loading)
    val uiState: StateFlow<CollectorListUiState> = _uiState.asStateFlow()

    init {
        loadCollectors()
    }

    /**
     * Pide los coleccionistas. Se llama al abrir la pantalla (usa la caché) y desde el botón
     * "Reintentar" (con [forceRefresh] en true para volver a ir a la red).
     */
    fun loadCollectors(forceRefresh: Boolean = false) {
        _uiState.value = CollectorListUiState.Loading
        viewModelScope.launch {
            _uiState.value = collectorRepository.getCollectors(forceRefresh).fold(
                onSuccess = { collectors ->
                    if (collectors.isEmpty()) CollectorListUiState.Empty
                    else CollectorListUiState.Success(collectors)
                },
                onFailure = { error ->
                    CollectorListUiState.Error(error.message.orEmpty())
                }
            )
        }
    }

    companion object {
        /** Crea el ViewModel con el repositorio de AppContainer. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as VinilosApplication
                CollectorListViewModel(app.appContainer.collectorRepository)
            }
        }
    }
}
