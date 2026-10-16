package co.edu.uniandes.misw4203.vinilos.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.uniandes.misw4203.vinilos.data.ConnectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ConnectionUiState {
    data object Loading : ConnectionUiState
    data object Connected : ConnectionUiState
    data class Error(val message: String) : ConnectionUiState
}

class ConnectionViewModel(
    private val repository: ConnectionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConnectionUiState>(
        ConnectionUiState.Loading
    )
    val uiState: StateFlow<ConnectionUiState> = _uiState.asStateFlow()

    init {
        checkConnection()
    }

    fun checkConnection() {
        _uiState.value = ConnectionUiState.Loading

        viewModelScope.launch {
            repository.checkBackend()
                .onSuccess {
                    _uiState.value = ConnectionUiState.Connected
                }
                .onFailure { error ->
                    _uiState.value = ConnectionUiState.Error(
                        error.message ?: "Error desconocido"
                    )
                }
        }
    }
}