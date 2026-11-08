package co.edu.uniandes.misw4203.vinilos.ui.artists

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import co.edu.uniandes.misw4203.vinilos.R
import co.edu.uniandes.misw4203.vinilos.databinding.ItemArtistCountBinding

/**
 * Encabezado de la lista ("4 artistas"). Va en un adapter aparte,
 * unido al de los artistas con ConcatAdapter, para que se desplace con la lista.
 */
class ArtistCountAdapter : RecyclerView.Adapter<ArtistCountAdapter.CountViewHolder>() {

    var count: Int = 0
        set(value) {
            if (field != value) {
                field = value
                notifyItemChanged(0)
            }
        }

    override fun getItemCount() = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CountViewHolder {
        val binding = ItemArtistCountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CountViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CountViewHolder, position: Int) {
        val resources = holder.binding.root.resources
        holder.binding.artistCount.text =
            resources.getQuantityString(R.plurals.artists_count, count, count)
    }

    class CountViewHolder(val binding: ItemArtistCountBinding) : RecyclerView.ViewHolder(binding.root)
}
