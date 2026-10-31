package co.edu.uniandes.misw4203.vinilos.ui.role

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionViewModelTest {

    @Test
    fun `sin rol elegido el rol es null`() {
        val viewModel = SessionViewModel(SavedStateHandle())

        assertNull(viewModel.role.value)
    }

    @Test
    fun `selectRole guarda el rol elegido`() {
        val viewModel = SessionViewModel(SavedStateHandle())

        viewModel.selectRole(UserRole.COLLECTOR)

        assertEquals(UserRole.COLLECTOR, viewModel.role.value)
    }

    @Test
    fun `clearRole borra el rol`() {
        val viewModel = SessionViewModel(SavedStateHandle())
        viewModel.selectRole(UserRole.VISITOR)

        viewModel.clearRole()

        assertNull(viewModel.role.value)
    }

    @Test
    fun `el rol se recupera del estado guardado`() {
        val handle = SavedStateHandle()
        SessionViewModel(handle).selectRole(UserRole.VISITOR)

        val restored = SessionViewModel(handle)

        assertEquals(UserRole.VISITOR, restored.role.value)
    }
}
