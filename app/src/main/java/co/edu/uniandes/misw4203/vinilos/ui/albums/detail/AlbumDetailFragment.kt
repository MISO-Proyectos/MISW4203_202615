package co.edu.uniandes.misw4203.vinilos.ui.albums.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import co.edu.uniandes.misw4203.vinilos.R
import co.edu.uniandes.misw4203.vinilos.data.model.Album
import co.edu.uniandes.misw4203.vinilos.data.model.Comment
import co.edu.uniandes.misw4203.vinilos.data.model.Track
import co.edu.uniandes.misw4203.vinilos.databinding.FragmentAlbumDetailBinding
import co.edu.uniandes.misw4203.vinilos.databinding.ItemCommentBinding
import co.edu.uniandes.misw4203.vinilos.databinding.ItemTrackBinding
import co.edu.uniandes.misw4203.vinilos.ui.role.SessionViewModel
import co.edu.uniandes.misw4203.vinilos.ui.role.UserRole
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Pantalla de detalle de un álbum (HU02). Solo dibuja el estado que entrega
 * AlbumDetailViewModel; el botón "Agregar track" depende del rol de SessionViewModel.
 * Las listas de tracks y comentarios son cortas y van dentro del scroll, por eso
 * se dibujan como filas en un LinearLayout en vez de un RecyclerView.
 */
class AlbumDetailFragment : Fragment() {

    private var _binding: FragmentAlbumDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AlbumDetailViewModel by viewModels { AlbumDetailViewModel.Factory }

    /** Rol elegido en la primera pantalla; es el mismo ViewModel que usa MainActivity. */
    private val session: SessionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlbumDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.retryButton.setOnClickListener { viewModel.loadAlbum() }
        binding.addTrackButton.setOnClickListener {
            // TODO: abrir el formulario para asociar un track cuando exista su HU
            Snackbar.make(binding.root, R.string.add_track_coming_soon, Snackbar.LENGTH_SHORT).show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { session.role.collect(::renderRole) }
            }
        }
    }

    private fun renderRole(role: UserRole?) {
        binding.addTrackButton.isVisible = role?.canAddTracks == true
    }

    private fun render(state: AlbumDetailUiState) {
        binding.loadingIndicator.isVisible = state is AlbumDetailUiState.Loading
        binding.detailContent.isVisible = state is AlbumDetailUiState.Success
        binding.errorContainer.isVisible = state is AlbumDetailUiState.Error

        when (state) {
            is AlbumDetailUiState.Success -> showAlbum(state.album)
            is AlbumDetailUiState.Error ->
                binding.errorText.text =
                    state.message.ifBlank { getString(R.string.albums_error_default) }
            AlbumDetailUiState.Loading -> Unit
        }
    }

    private fun showAlbum(album: Album) {
        val unknown = getString(R.string.album_value_unknown)

        binding.albumName.text = album.name
        binding.albumPerformers.text = album.performerNames
            .ifBlank { getString(R.string.album_unknown_performer) }
        binding.albumCover.contentDescription = getString(R.string.album_cover_description, album.name)
        // Se ata a la vista (no al fragment) para que la carga se cancele al destruir la vista.
        // La carátula se decodifica al tamaño con que se dibuja, no al de la imagen original.
        Glide.with(binding.albumCover)
            .load(album.cover.ifBlank { null })
            .placeholder(R.drawable.ic_album)
            .error(R.drawable.ic_album)
            .override(resources.getDimensionPixelSize(R.dimen.album_detail_cover_size))
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .into(binding.albumCover)

        val months = resources.getStringArray(R.array.months_short).toList()
        binding.releaseDateValue.text = album.releaseDate
            ?.let { AlbumDetailFormatter.formatReleaseDate(it, months) } ?: unknown
        binding.genreValue.text = album.genre.ifBlank { unknown }
        binding.recordLabelValue.text = album.recordLabel.ifBlank { unknown }
        binding.albumDescription.text = album.description
            .ifBlank { getString(R.string.album_no_description) }

        showTracks(album.tracks)
        showComments(album.comments)
    }

    private fun showTracks(tracks: List<Track>) {
        binding.tracksCount.text =
            resources.getQuantityString(R.plurals.album_tracks_count, tracks.size, tracks.size)
        binding.tracksContainer.isVisible = tracks.isNotEmpty()
        binding.tracksEmpty.isVisible = tracks.isEmpty()

        binding.tracksContainer.removeAllViews()
        tracks.forEachIndexed { index, track ->
            val row = ItemTrackBinding.inflate(layoutInflater, binding.tracksContainer, true)
            val duration = AlbumDetailFormatter.formatDuration(track.duration)
            row.trackDivider.isVisible = index > 0
            row.trackNumber.text = AlbumDetailFormatter.formatTrackNumber(index + 1)
            row.trackName.text = track.name
            row.trackDuration.text = duration
            // TalkBack lee la fila completa en una sola frase
            row.trackRow.contentDescription =
                getString(R.string.track_description, index + 1, track.name, duration)
        }
    }

    private fun showComments(comments: List<Comment>) {
        binding.commentsCount.text = AlbumDetailFormatter.formatCount(comments.size)
        binding.commentsContainer.isVisible = comments.isNotEmpty()
        binding.commentsEmpty.isVisible = comments.isEmpty()

        binding.commentsContainer.removeAllViews()
        // Colores y tamaños de las estrellas: se resuelven una vez para todos los comentarios
        val starStyle = StarStyle.from(binding.commentsContainer)
        comments.forEachIndexed { index, comment ->
            val row = ItemCommentBinding.inflate(layoutInflater, binding.commentsContainer, true)
            row.commentDivider.isVisible = index > 0
            row.commentText.text = comment.description
            showStars(row.commentStars, comment.rating, starStyle)
        }
    }

    /** Dibuja cinco estrellas: las primeras [rating] en naranja y el resto apagadas. */
    private fun showStars(container: LinearLayout, rating: Int, style: StarStyle) {
        container.removeAllViews()
        container.contentDescription = getString(R.string.comment_rating_description, rating)
        repeat(MAX_RATING) { position ->
            val star = ImageView(container.context).apply {
                setImageResource(R.drawable.ic_star)
                setColorFilter(if (position < rating) style.filledColor else style.emptyColor)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            val params = LinearLayout.LayoutParams(style.size, style.size).apply { marginEnd = style.gap }
            container.addView(star, params)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    /** Colores y medidas de las estrellas, iguales para todos los comentarios. */
    private class StarStyle(val filledColor: Int, val emptyColor: Int, val size: Int, val gap: Int) {
        companion object {
            fun from(view: View): StarStyle {
                val resources = view.resources
                return StarStyle(
                    filledColor = MaterialColors.getColor(view, androidx.appcompat.R.attr.colorPrimary),
                    emptyColor = MaterialColors.getColor(view, com.google.android.material.R.attr.colorOutlineVariant),
                    size = resources.getDimensionPixelSize(R.dimen.comment_star_size),
                    gap = resources.getDimensionPixelSize(R.dimen.comment_star_gap)
                )
            }
        }
    }

    private companion object {
        const val MAX_RATING = 5
    }
}
