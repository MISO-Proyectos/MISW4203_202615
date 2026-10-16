package co.edu.uniandes.misw4203.vinilos.data

import co.edu.uniandes.misw4203.vinilos.core.network.BackendApi

class ConnectionRepository(
    private val backendApi: BackendApi
) {
    suspend fun checkBackend(): Result<Unit> = runCatching {
        val response = backendApi.getAlbumsForHealthCheck()
        response.body()?.close()
        response.errorBody()?.close()

        if (!response.isSuccessful) {
            error("El backend respondió HTTP ${response.code()}")
        }
    }
}