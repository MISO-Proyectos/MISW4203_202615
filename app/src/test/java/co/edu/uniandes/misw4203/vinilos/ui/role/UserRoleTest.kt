package co.edu.uniandes.misw4203.vinilos.ui.role

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserRoleTest {

    @Test
    fun `el coleccionista puede agregar albumes`() {
        assertTrue(UserRole.COLLECTOR.canAddAlbums)
    }

    @Test
    fun `el visitante no puede agregar albumes`() {
        assertFalse(UserRole.VISITOR.canAddAlbums)
    }

    @Test
    fun `solo el coleccionista puede agregar tracks`() {
        assertTrue(UserRole.COLLECTOR.canAddTracks)
        assertFalse(UserRole.VISITOR.canAddTracks)
    }
}
