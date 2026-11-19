package co.edu.uniandes.misw4203.vinilos.ui.role

import androidx.annotation.StringRes
import co.edu.uniandes.misw4203.vinilos.R

/**
 * Roles con los que el usuario puede entrar a la app y lo que cada uno puede hacer.
 * Las pantallas preguntan por el permiso (por ejemplo [canAddAlbums]) y no por el rol,
 * así un rol nuevo solo se agrega aquí.
 */
enum class UserRole(
    @param:StringRes val labelRes: Int,
    /** Muestra el botón "Agregar álbum" en el catálogo. */
    val canAddAlbums: Boolean,
    /** Muestra el botón "Agregar track" en el detalle de un álbum. */
    val canAddTracks: Boolean,
    /** Muestra el botón "Comentar" en el detalle de un álbum. */
    val canAddComments: Boolean,
    /** Muestra el botón "Agregar favorito" en el detalle de un artista (HU10). */
    val canAddFavorites: Boolean,
    /** Muestra el botón "Agregar a colección" en el detalle de un álbum (HU11). */
    val canAddToCollection: Boolean
) {
    VISITOR(R.string.role_visitor, canAddAlbums = false, canAddTracks = false, canAddComments = false,
        canAddFavorites = false, canAddToCollection = false),
    COLLECTOR(R.string.role_collector, canAddAlbums = true, canAddTracks = true, canAddComments = true,
        canAddFavorites = true, canAddToCollection = true)
}
