package co.edu.uniandes.misw4203.vinilos.ui.albums

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import co.edu.uniandes.misw4203.vinilos.R
import co.edu.uniandes.misw4203.vinilos.databinding.ItemAlbumCountBinding

/**
 * Encabezado de la lista ("4 álbumes en el catálogo"). Va en un adapter aparte,
 * unido al de los álbumes con ConcatAdapter, para que se desplace con la lista.
 */
class AlbumCountAdapter : RecyclerView.Adapter<AlbumCountAdapter.CountViewHolder>() {

    var count: Int = 0
        set(value) {
            if (field != value) {
                field = value
                notifyItemChanged(0)
            }
        }

    override fun getItemCount() = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CountViewHolder {
        val binding = ItemAlbumCountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CountViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CountViewHolder, position: Int) {
        val resources = holder.binding.root.resources
        holder.binding.albumCount.text =
            resources.getQuantityString(R.plurals.albums_count, count, count)
    }

    class CountViewHolder(val binding: ItemAlbumCountBinding) : RecyclerView.ViewHolder(binding.root)
}
