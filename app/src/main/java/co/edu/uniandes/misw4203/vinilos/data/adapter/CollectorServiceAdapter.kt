package co.edu.uniandes.misw4203.vinilos.data.adapter

import co.edu.uniandes.misw4203.vinilos.core.network.BackendApi
import co.edu.uniandes.misw4203.vinilos.data.adapter.AlbumServiceAdapter.Companion.toModel
import co.edu.uniandes.misw4203.vinilos.data.dto.CollectorAlbumCreateDto
import co.edu.uniandes.misw4203.vinilos.data.dto.CollectorAlbumDto
import co.edu.uniandes.misw4203.vinilos.data.dto.CollectorDto
import co.edu.uniandes.misw4203.vinilos.data.model.ArtistType
import co.edu.uniandes.misw4203.vinilos.data.model.Collector
import co.edu.uniandes.misw4203.vinilos.data.model.CollectorAlbum
import co.edu.uniandes.misw4203.vinilos.data.model.CollectorAlbumStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Service Adapter de coleccionistas: llama a la API REST y convierte el JSON (DTO)
 * en los modelos de la app. Los errores de red se propagan;
 * CollectorRepository los traduce. La conversión DTO → modelo corre en [dispatcher]
 * (por defecto Dispatchers.Default).
 */
class CollectorServiceAdapter(
    private val api: BackendApi,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {

    /** HU05: listado de coleccionistas. Descarta los que lleguen sin id. */
    suspend fun getCollectors(): List<Collector> {
        val dtos = api.getCollectors()
        return withContext(dispatcher) { dtos.mapNotNull { it.toModel() } }
    }

    /**
     * HU06: detalle de un coleccionista. Pide en paralelo GET /collectors/{id} (datos, favoritos
     * y comentarios) y GET /collectors/{id}/albums (la colección con el nombre y la carátula
     * de cada álbum, que la primera ruta no trae). Si una falla, falla todo.
     */
    suspend fun getCollector(id: Int): Collector = coroutineScope {
        val detail = async { api.getCollector(id) }
        val albums = async { api.getCollectorAlbums(id) }
        val detailDto = detail.await()
        val albumDtos = albums.await()
        withContext(dispatcher) {
            detailDto.toDetailModel(albumDtos.mapNotNull { it.toModel() })
                ?: throw IllegalStateException("El backend devolvió un coleccionista sin id")
        }
    }

    /** HU11: ids de los álbumes que ya están en la colección del coleccionista. */
    suspend fun getCollectorAlbumIds(collectorId: Int): Set<Int> {
        val dtos = api.getCollectorAlbums(collectorId)
        return withContext(dispatcher) { dtos.mapNotNull { it.album?.id }.toSet() }
    }

    /** HU11: agrega el álbum a la colección del coleccionista (POST). Los errores se propagan. */
    suspend fun addAlbumToCollector(
        collectorId: Int,
        albumId: Int,
        price: Int,
        status: CollectorAlbumStatus
    ) {
        api.addAlbumToCollector(
            collectorId,
            albumId,
            CollectorAlbumCreateDto(price = price, status = status.apiValue)
        )
    }

    /** HU10: ids de los artistas (músicos y bandas) que ya son favoritos del coleccionista. */
    suspend fun getFavoriteArtistIds(collectorId: Int): Set<Int> {
        val dtos = api.getCollectorPerformers(collectorId)
        return withContext(dispatcher) { dtos.mapNotNull { it.id }.toSet() }
    }

    /** HU10: agrega el artista a los favoritos del coleccionista (POST según su tipo). Los errores se propagan. */
    suspend fun addFavoriteArtist(collectorId: Int, artistId: Int, type: ArtistType) {
        when (type) {
            ArtistType.MUSICIAN -> api.addFavoriteMusician(collectorId, artistId)
            ArtistType.BAND -> api.addFavoriteBand(collectorId, artistId)
        }
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
