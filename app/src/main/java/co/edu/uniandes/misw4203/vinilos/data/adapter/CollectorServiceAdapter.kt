package co.edu.uniandes.misw4203.vinilos.data.adapter

import co.edu.uniandes.misw4203.vinilos.core.network.BackendApi
import co.edu.uniandes.misw4203.vinilos.data.adapter.AlbumServiceAdapter.Companion.toModel
import co.edu.uniandes.misw4203.vinilos.data.dto.CollectorAlbumDto
import co.edu.uniandes.misw4203.vinilos.data.dto.CollectorDto
import co.edu.uniandes.misw4203.vinilos.data.model.Collector
import co.edu.uniandes.misw4203.vinilos.data.model.CollectorAlbum
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

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

    /**
     * HU06: detalle de un coleccionista. Pide en paralelo GET /collectors/{id} (datos, favoritos
     * y comentarios) y GET /collectors/{id}/albums (la colección con el nombre y la carátula
     * de cada álbum, que la primera ruta no trae). Si una falla, falla todo.
     */
    suspend fun getCollector(id: Int): Collector = coroutineScope {
        val detail = async { api.getCollector(id) }
        val albums = async { api.getCollectorAlbums(id) }
        detail.await().toDetailModel(albums.await().mapNotNull { it.toModel() })
            ?: throw IllegalStateException("El backend devolvió un coleccionista sin id")
    }

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

        /** Convierte el detalle del JSON en modelo con su colección ya convertida. */
        fun CollectorDto.toDetailModel(albums: List<CollectorAlbum>): Collector? {
            val base = toModel() ?: return null
            return base.copy(
                albums = albums,
                favoritePerformers = favoritePerformers.orEmpty().mapNotNull { it.toModel() },
                comments = comments.orEmpty().mapNotNull { it.toModel() }
            )
        }

        /** Convierte un álbum de la colección. Descarta los que lleguen sin id o sin álbum. */
        fun CollectorAlbumDto.toModel(): CollectorAlbum? {
            val relationId = id ?: return null
            val albumModel = album?.toModel() ?: return null
            return CollectorAlbum(
                id = relationId,
                price = price ?: 0,
                status = status.orEmpty(),
                album = albumModel
            )
        }
    }
}
