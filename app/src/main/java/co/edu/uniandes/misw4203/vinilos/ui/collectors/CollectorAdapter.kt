package co.edu.uniandes.misw4203.vinilos.ui.collectors

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import co.edu.uniandes.misw4203.vinilos.data.model.Collector
import co.edu.uniandes.misw4203.vinilos.databinding.ItemCollectorBinding

/** Adapter de la lista de coleccionistas: dibuja una tarjeta por coleccionista y avisa cuando se toca una. */
class CollectorAdapter(
    private val onCollectorClick: (Collector) -> Unit
) : ListAdapter<Collector, CollectorAdapter.CollectorViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectorViewHolder {
        val binding = ItemCollectorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CollectorViewHolder(binding, onCollectorClick)
    }

    override fun onBindViewHolder(holder: CollectorViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CollectorViewHolder(
        private val binding: ItemCollectorBinding,
        onCollectorClick: (Collector) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var collector: Collector? = null

        init {
            binding.collectorCard.setOnClickListener { collector?.let(onCollectorClick) }
        }

        fun bind(collector: Collector) {
            this.collector = collector

            binding.collectorAvatar.text = collector.initials
            binding.collectorName.text = collector.name
            binding.collectorEmail.text = collector.email
            binding.collectorEmail.isVisible = collector.email.isNotBlank()
            binding.collectorTelephone.text = collector.telephone
            binding.collectorTelephone.isVisible = collector.telephone.isNotBlank()

            // TalkBack lee la tarjeta completa en una sola frase
            binding.collectorCard.contentDescription =
                listOf(collector.name, collector.email, collector.telephone)
                    .filter { it.isNotBlank() }
                    .joinToString(", ")
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<Collector>() {
            override fun areItemsTheSame(oldItem: Collector, newItem: Collector) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Collector, newItem: Collector) = oldItem == newItem
        }
    }
}
