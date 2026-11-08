package co.edu.uniandes.misw4203.vinilos.ui.artists

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import co.edu.uniandes.misw4203.vinilos.R
import co.edu.uniandes.misw4203.vinilos.data.model.Artist
import co.edu.uniandes.misw4203.vinilos.data.model.ArtistType
import co.edu.uniandes.misw4203.vinilos.databinding.ItemArtistBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

/** Adapter de la lista de artistas: dibuja una tarjeta por artista y avisa cuando se toca una. */
class ArtistAdapter(
    private val onArtistClick: (Artist) -> Unit
) : ListAdapter<Artist, ArtistAdapter.ArtistViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
        val binding = ItemArtistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ArtistViewHolder(binding, onArtistClick)
    }

    override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: ArtistViewHolder) {
        // Cancela la descarga de la foto si la tarjeta sale de la pantalla. Se usa el contexto
        // de la aplicación porque al cerrar la actividad Glide ya no acepta Glide.with(vista).
        val image = holder.binding.artistImage
        Glide.with(image.context.applicationContext).clear(image)
        super.onViewRecycled(holder)
    }

    class ArtistViewHolder(
        val binding: ItemArtistBinding,
        onArtistClick: (Artist) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var artist: Artist? = null

        init {
            binding.artistCard.setOnClickListener { artist?.let(onArtistClick) }
        }

        fun bind(artist: Artist) {
            this.artist = artist
            val type = binding.root.context.getString(artist.type.labelRes)

            binding.artistName.text = artist.name
            binding.artistType.text = type

            // TalkBack lee la tarjeta completa en una sola frase
            binding.artistCard.contentDescription =
                listOf(artist.name, type).filter { it.isNotBlank() }.joinToString(", ")

            Glide.with(binding.artistImage)
                .load(artist.image.ifBlank { null })
                .placeholder(R.drawable.ic_artist)
                .error(R.drawable.ic_artist)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(binding.artistImage)
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<Artist>() {
            override fun areItemsTheSame(oldItem: Artist, newItem: Artist) =
                oldItem.id == newItem.id && oldItem.type == newItem.type

            override fun areContentsTheSame(oldItem: Artist, newItem: Artist) = oldItem == newItem
        }
    }
}

/** Texto que se muestra debajo del nombre del artista. */
@get:StringRes
val ArtistType.labelRes: Int
    get() = when (this) {
        ArtistType.MUSICIAN -> R.string.artist_type_musician
        ArtistType.BAND -> R.string.artist_type_band
    }
