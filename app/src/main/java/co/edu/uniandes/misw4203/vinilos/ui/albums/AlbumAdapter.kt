package co.edu.uniandes.misw4203.vinilos.ui.albums

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import co.edu.uniandes.misw4203.vinilos.R
import co.edu.uniandes.misw4203.vinilos.data.model.Album
import co.edu.uniandes.misw4203.vinilos.databinding.ItemAlbumBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

/** Adapter de la lista de álbumes: dibuja una tarjeta por álbum. */
class AlbumAdapter : ListAdapter<Album, AlbumAdapter.AlbumViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val binding = ItemAlbumBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AlbumViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: AlbumViewHolder) {
        // Cancela la descarga de la carátula si la tarjeta sale de la pantalla
        Glide.with(holder.binding.albumCover).clear(holder.binding.albumCover)
        super.onViewRecycled(holder)
    }

    class AlbumViewHolder(val binding: ItemAlbumBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(album: Album) {
            val context = binding.root.context
            val performers = album.performerNames
                .ifBlank { context.getString(R.string.album_unknown_performer) }
            val year = album.releaseYear?.toString().orEmpty()

            binding.albumName.text = album.name
            binding.albumPerformers.text = performers
            binding.albumGenre.text = album.genre
            binding.albumGenre.isVisible = album.genre.isNotBlank()
            binding.albumYear.text = year
            binding.albumYear.isVisible = year.isNotEmpty()

            // TalkBack lee la tarjeta completa en una sola frase
            binding.albumCard.contentDescription =
                listOf(album.name, performers, album.genre, year)
                    .filter { it.isNotBlank() }
                    .joinToString(", ")

            Glide.with(binding.albumCover)
                .load(album.cover.ifBlank { null })
                .placeholder(R.drawable.ic_album)
                .error(R.drawable.ic_album)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(binding.albumCover)
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<Album>() {
            override fun areItemsTheSame(oldItem: Album, newItem: Album) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Album, newItem: Album) = oldItem == newItem
        }
    }
}
