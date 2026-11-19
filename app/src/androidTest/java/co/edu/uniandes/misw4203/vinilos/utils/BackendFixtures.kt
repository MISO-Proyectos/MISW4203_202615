package co.edu.uniandes.misw4203.vinilos.utils

import androidx.test.core.app.ApplicationProvider
import co.edu.uniandes.misw4203.vinilos.BuildConfig
import co.edu.uniandes.misw4203.vinilos.VinilosApplication
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Datos de prueba que las E2E crean por la API del backend real, para no cambiar los datos
 * por defecto (álbumes 100 a 103, coleccionistas 100 y 101) que revisan las pruebas de
 * HU02, HU05 y HU06. Se llaman desde el hilo de la prueba, después de conceder el permiso
 * de red local, nunca desde el hilo principal.
 */
object BackendFixtures {

    private val appContainer
        get() = ApplicationProvider.getApplicationContext<VinilosApplication>().appContainer

    /** Crea un álbum válido (POST /albums) y devuelve su id. Limpia la caché del catálogo. */
    fun crearAlbum(nombre: String): Int {
        val body = JSONObject()
            .put("name", nombre)
            .put("cover", "https://i.pinimg.com/564x/aa/5f/ed/aa5fed7fac61cc8f41d1e79db917a7cd.jpg")
            .put("releaseDate", "2020-01-01T00:00:00.000Z")
            .put("description", "Álbum creado por una prueba E2E.")
            .put("genre", "Rock")
            .put("recordLabel", "EMI")
        val id = enviar("albums", body, "el álbum \"$nombre\"")
        appContainer.albumRepository.clearCache()
        return id
    }

    /**
     * Crea un coleccionista (POST /collectors) con teléfono y correo únicos, porque el
     * backend exige los tres campos y valida el correo. Devuelve su id y limpia la caché.
     */
    fun crearColeccionista(nombre: String): Int {
        val sufijo = System.nanoTime()
        val body = JSONObject()
            .put("name", nombre)
            .put("telephone", "3${sufijo % 1_000_000_000L}")
            .put("email", "e2e$sufijo@vinilos.test")
        val id = enviar("collectors", body, "el coleccionista \"$nombre\"")
        appContainer.collectorRepository.clearCache()
        return id
    }

    /** Cuenta los coleccionistas que hay hoy en el backend (GET /collectors). */
    fun contarColeccionistas(): Int {
        val connection = abrir("collectors")
        try {
            check(connection.responseCode in 200..299) {
                "No se pudieron consultar los coleccionistas (HTTP ${connection.responseCode})"
            }
            return JSONArray(connection.inputStream.bufferedReader().use { it.readText() }).length()
        } finally {
            connection.disconnect()
        }
    }

    private fun abrir(ruta: String) =
        URL(BuildConfig.BASE_URL + ruta).openConnection() as HttpURLConnection

    private fun enviar(ruta: String, body: JSONObject, descripcion: String): Int {
        val connection = abrir(ruta)
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = connection.responseCode
            check(code in 200..299) { "No se pudo crear $descripcion (HTTP $code)" }
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(response).getInt("id")
        } finally {
            connection.disconnect()
        }
    }
}
