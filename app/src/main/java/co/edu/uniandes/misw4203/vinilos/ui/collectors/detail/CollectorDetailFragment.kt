package co.edu.uniandes.misw4203.vinilos.ui.collectors.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import co.edu.uniandes.misw4203.vinilos.R
import co.edu.uniandes.misw4203.vinilos.data.model.Collector
import co.edu.uniandes.misw4203.vinilos.data.model.CollectorAlbum
import co.edu.uniandes.misw4203.vinilos.data.model.Comment
import co.edu.uniandes.misw4203.vinilos.data.model.Performer
import co.edu.uniandes.misw4203.vinilos.databinding.FragmentCollectorDetailBinding
import co.edu.uniandes.misw4203.vinilos.databinding.ItemCollectorAlbumBinding
import co.edu.uniandes.misw4203.vinilos.databinding.ItemCommentBinding
import co.edu.uniandes.misw4203.vinilos.ui.albums.detail.AlbumDetailViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.launch

/**
 * Pantalla de detalle de un coleccionista (HU06). Solo dibuja el estado de
 * CollectorDetailViewModel. Tocar un álbum de la colección abre su detalle (HU02);
 * los artistas favoritos no se abren porque el backend no dice si son músicos o bandas.
 */
class CollectorDetailFragment : Fragment() {

    private var _binding: FragmentCollectorDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CollectorDetailViewModel by viewModels { CollectorDetailViewModel.Factory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCollectorDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.retryButton.setOnClickListener { viewModel.loadCollector() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: CollectorDetailUiState) {
        binding.loadingIndicator.isVisible = state is CollectorDetailUiState.Loading
        binding.detailContent.isVisible = state is CollectorDetailUiState.Success
        binding.errorContainer.isVisible = state is CollectorDetailUiState.Error
        when (state) {
            is CollectorDetailUiState.Success -> showCollector(state.collector)
            is CollectorDetailUiState.Error ->
                binding.errorText.text = state.message.ifBlank { getString(R.string.collectors_error_default) }
            CollectorDetailUiState.Loading -> Unit
        }
    }

    private fun showCollector(collector: Collector) {
        val unknown = getString(R.string.album_value_unknown)
        binding.detailAvatar.text = collector.initials
        binding.detailName.text = collector.name
        binding.detailTelephone.text = collector.telephone.ifBlank { unknown }
        binding.detailEmail.text = collector.email.ifBlank { unknown }
        showAlbums(collector.albums)
        showPerformers(collector.favoritePerformers)
        showComments(collector.comments)
    }

    private fun showAlbums(albums: List<CollectorAlbum>) {
        binding.albumsCount.text = CollectorDetailFormatter.formatCount(albums.size)
        binding.albumsContainer.isVisible = albums.isNotEmpty()
        binding.albumsEmpty.isVisible = albums.isEmpty()
        binding.albumsContainer.removeAllViews()
        // Tamaño de la carátula: se calcula una vez para todas las filas
        val coverSize = resources.getDimensionPixelSize(R.dimen.collector_album_cover_size)
        albums.forEachIndexed { index, item ->
            val row = ItemCollectorAlbumBinding.inflate(layoutInflater, binding.albumsContainer, true)
            val album = item.album
            row.albumDivider.isVisible = index > 0
            row.albumName.text = album.name
            row.albumPrice.text = getString(
                R.string.collector_album_price,
                CollectorDetailFormatter.formatPrice(item.price)
            )
            row.albumStatus.setText(
                if (CollectorDetailFormatter.isActive(item.status)) R.string.collector_album_status_active
                else R.string.collector_album_status_inactive
            )
            Glide.with(row.albumCover)
                .load(album.cover.ifBlank { null })
                .placeholder(R.drawable.ic_album)
                .error(R.drawable.ic_album)
                .override(coverSize)
                .format(DecodeFormat.PREFER_RGB_565)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(row.albumCover)
            row.albumRow.setOnClickListener { openAlbumDetail(album.id) }
        }
    }

    /** Artistas favoritos como chips con borde (sin acción: el backend no dice si son músicos o bandas). */
    private fun showPerformers(performers: List<Performer>) {
        binding.performersCount.text = CollectorDetailFormatter.formatCount(performers.size)
        binding.performersGroup.isVisible = performers.isNotEmpty()
        binding.performersEmpty.isVisible = performers.isEmpty()
        binding.performersGroup.removeAllViews()
        performers.forEach { performer ->
            val chip = Chip(requireContext()).apply {
                text = performer.name
                setChipIconResource(R.drawable.ic_mic)
                isChipIconVisible = true
                isClickable = false
                isCheckable = false
                isFocusable = false
            }
            binding.performersGroup.addView(chip)
        }
    }

    private fun showComments(comments: List<Comment>) {
        binding.commentsCount.text = CollectorDetailFormatter.formatCount(comments.size)
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

    /** Dibuja las cinco estrellas: llenas hasta [rating] y vacías el resto. */
    private fun showStars(container: LinearLayout, rating: Int, style: StarStyle) {
        container.removeAllViews()
        container.contentDescription = getString(R.string.comment_rating_description, rating)
        repeat(MAX_RATING) { position ->
            val star = ImageView(container.context).apply {
                setImageResource(R.drawable.ic_star)
                setColorFilter(if (position < rating) style.filledColor else style.emptyColor)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            container.addView(star, LinearLayout.LayoutParams(style.size, style.size).apply { marginEnd = style.gap })
        }
    }

    private fun openAlbumDetail(albumId: Int) {
        findNavController().navigate(
            R.id.action_collectorDetail_to_albumDetail,
            bundleOf(AlbumDetailViewModel.ARG_ALBUM_ID to albumId)
        )
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
