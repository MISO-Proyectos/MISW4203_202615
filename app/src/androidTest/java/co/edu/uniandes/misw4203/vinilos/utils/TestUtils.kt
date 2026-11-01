package co.edu.uniandes.misw4203.vinilos.utils

import android.os.Build
import android.os.SystemClock
import androidx.test.rule.GrantPermissionRule

object TestUtils {

    /**
     * Android 17 (API 37) pide permiso para conectarse a la red local (10.0.2.2, donde corre
     * el backend). Se concede antes de abrir la app para que el diálogo no tape la pantalla.
     * En versiones anteriores ese permiso no existe y la regla no hace nada.
     */
    fun grantLocalNetworkAccess(): GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= 37) {
            GrantPermissionRule.grant("android.permission.ACCESS_LOCAL_NETWORK")
        } else {
            GrantPermissionRule.grant()
        }

    /**
     * Repite la verificación hasta que pase o se agote el tiempo.
     * La respuesta de la red llega después de que la pantalla se abre,
     * así que no se puede verificar de inmediato.
     */
    fun waitFor(
        timeoutMs: Long = 10_000,
        intervalMs: Long = 250,
        check: () -> Unit
    ) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        var lastError: Throwable? = null

        while (SystemClock.elapsedRealtime() < deadline) {
            try {
                check()
                return
            } catch (e: Throwable) {
                lastError = e
                Thread.sleep(intervalMs)
            }
        }
        throw lastError ?: AssertionError("Tiempo de espera agotado")
    }
}
