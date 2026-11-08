package co.edu.uniandes.misw4203.vinilos.data.adapter

import co.edu.uniandes.misw4203.vinilos.core.network.BackendApi
import co.edu.uniandes.misw4203.vinilos.data.dto.CollectorDto
import co.edu.uniandes.misw4203.vinilos.data.model.Collector

/**
 * Service Adapter de coleccionistas: llama a la API REST y convierte el JSON (DTO)
 * en los modelos de la app. Los errores de red se propagan;
 * CollectorRepository los traduce.
 */
class CollectorServiceAdapter(
    private val api: BackendApi
) {

    /** HU05: listado de coleccionistas. Descarta los que lleguen sin id. */
    suspend fun getCollectors(): List<Collector> =
        api.getCollectors().mapNotNull { it.toModel() }

    companion object {

        /** Convierte un coleccionista del JSON en modelo. Descarta los que no traen id. */
        fun CollectorDto.toModel(): Collector? {
            val collectorId = id ?: return null
            return Collector(
                id = collectorId,
                name = name.orEmpty(),
                telephone = telephone.orEmpty(),
                email = email.orEmpty()
            )
        }
    }
}
