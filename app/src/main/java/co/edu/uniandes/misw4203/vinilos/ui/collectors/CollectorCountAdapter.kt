package co.edu.uniandes.misw4203.vinilos.ui.collectors

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import co.edu.uniandes.misw4203.vinilos.R
import co.edu.uniandes.misw4203.vinilos.databinding.ItemCollectorCountBinding

/**
 * Encabezado de la lista ("2 coleccionistas"). Va en un adapter aparte,
 * unido al de los coleccionistas con ConcatAdapter, para que se desplace con la lista.
 */
class CollectorCountAdapter : RecyclerView.Adapter<CollectorCountAdapter.CountViewHolder>() {

    var count: Int = 0
        set(value) {
            if (field != value) {
                field = value
                notifyItemChanged(0)
            }
        }

    override fun getItemCount() = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CountViewHolder {
        val binding = ItemCollectorCountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CountViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CountViewHolder, position: Int) {
        val resources = holder.binding.root.resources
        holder.binding.collectorCount.text =
            resources.getQuantityString(R.plurals.collectors_count, count, count)
    }

    class CountViewHolder(val binding: ItemCollectorCountBinding) : RecyclerView.ViewHolder(binding.root)
}
