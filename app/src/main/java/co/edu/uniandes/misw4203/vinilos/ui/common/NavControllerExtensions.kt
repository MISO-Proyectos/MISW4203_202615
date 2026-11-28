package co.edu.uniandes.misw4203.vinilos.ui.common

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavController

/**
 * Navega solo si el destino actual todavía tiene la acción. Con dos toques seguidos, el
 * segundo llega cuando el destino ya cambió y navigate() cerraría la app.
 */
fun NavController.navigateOnce(@IdRes actionId: Int, args: Bundle? = null) {
    if (currentDestination?.getAction(actionId) == null) return
    navigate(actionId, args)
}
