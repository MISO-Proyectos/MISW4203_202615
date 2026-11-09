package co.edu.uniandes.misw4203.vinilos.ui.collectors

import co.edu.uniandes.misw4203.vinilos.data.model.Collector

/** Estados posibles de la pantalla del listado de coleccionistas (HU05). */
sealed interface CollectorListUiState {

    /** Se están pidiendo los coleccionistas al backend. */
    data object Loading : CollectorListUiState

    /** El backend respondió con al menos un coleccionista. */
    data class Success(val collectors: List<Collector>) : CollectorListUiState

    /** El backend respondió bien, pero no hay coleccionistas registrados. */
    data object Empty : CollectorListUiState

    /** No se pudieron cargar los coleccionistas; [message] se muestra al usuario. */
    data class Error(val message: String) : CollectorListUiState
}
