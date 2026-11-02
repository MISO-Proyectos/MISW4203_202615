package co.edu.uniandes.misw4203.vinilos.ui.role

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Guarda el rol que eligió el usuario. Lo comparten la actividad y los fragments
 * (con activityViewModels), así la pantalla de rol y la barra superior leen el mismo valor.
 * El rol se guarda también en SavedStateHandle para no perderlo si Android cierra el proceso.
 */
class SessionViewModel(
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : ViewModel() {

    private val _role = MutableStateFlow(
        savedStateHandle.get<String>(KEY_ROLE)?.let { name ->
            UserRole.entries.firstOrNull { it.name == name }
        }
    )

    /** Rol actual; es null mientras el usuario no haya elegido uno. */
    val role: StateFlow<UserRole?> = _role.asStateFlow()

    fun selectRole(role: UserRole) {
        _role.value = role
        savedStateHandle[KEY_ROLE] = role.name
    }

    fun clearRole() {
        _role.value = null
        savedStateHandle.remove<String>(KEY_ROLE)
    }

    private companion object {
        const val KEY_ROLE = "session_role"
    }
}
