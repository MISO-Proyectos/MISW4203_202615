package co.edu.uniandes.misw4203.vinilos.ui.collectors.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
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
 * HU06: pide el detalle del coleccionista cuyo id llega por la navegación (SavedStateHandle)
 * y expone el estado de la pantalla. Sobrevive a la rotación, así que no vuelve a cargar.
 */
class CollectorDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val collectorRepository: CollectorRepository
) : ViewModel() {

    private val collectorId: Int = requireNotNull(savedStateHandle.get<Int>(ARG_COLLECTOR_ID)) {
        "CollectorDetailViewModel necesita '$ARG_COLLECTOR_ID'"
    }

    private val _uiState = MutableStateFlow<CollectorDetailUiState>(CollectorDetailUiState.Loading)
    val uiState: StateFlow<CollectorDetailUiState> = _uiState.asStateFlow()

    init {
        loadCollector()
    }

    /** Pide el coleccionista; también lo usa el botón "Reintentar". */
    fun loadCollector() {
        _uiState.value = CollectorDetailUiState.Loading
        viewModelScope.launch {
            _uiState.value = collectorRepository.getCollector(collectorId).fold(
                onSuccess = { CollectorDetailUiState.Success(it) },
                onFailure = { CollectorDetailUiState.Error(it.message.orEmpty()) }
            )
        }
    }

    companion object {
        /** Nombre del argumento en nav_graph.xml. */
        const val ARG_COLLECTOR_ID = "collectorId"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as VinilosApplication
                CollectorDetailViewModel(createSavedStateHandle(), app.appContainer.collectorRepository)
            }
        }
    }
}
