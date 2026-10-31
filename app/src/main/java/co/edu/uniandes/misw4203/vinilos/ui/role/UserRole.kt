package co.edu.uniandes.misw4203.vinilos.ui.role

import androidx.annotation.StringRes
import co.edu.uniandes.misw4203.vinilos.R

/**
 * Roles con los que el usuario puede entrar a la app.
 * En el Sprint 1 el rol solo se elige y se muestra en la barra superior.
 */
enum class UserRole(@param:StringRes val labelRes: Int) {
    VISITOR(R.string.role_visitor),
    COLLECTOR(R.string.role_collector)
}
