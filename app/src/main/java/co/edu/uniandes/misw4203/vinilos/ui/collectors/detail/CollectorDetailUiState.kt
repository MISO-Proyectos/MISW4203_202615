package co.edu.uniandes.misw4203.vinilos.ui.collectors.detail

import co.edu.uniandes.misw4203.vinilos.data.model.Collector

/** Estados de la pantalla de detalle de un coleccionista (HU06). */
sealed interface CollectorDetailUiState {
    data object Loading : CollectorDetailUiState
    data class Success(val collector: Collector) : CollectorDetailUiState
    data class Error(val message: String) : CollectorDetailUiState
}
