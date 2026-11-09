package co.edu.uniandes.misw4203.vinilos.ui.artists.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import co.edu.uniandes.misw4203.vinilos.R
import co.edu.uniandes.misw4203.vinilos.data.model.Album
import co.edu.uniandes.misw4203.vinilos.data.model.ArtistDetail
import co.edu.uniandes.misw4203.vinilos.data.model.ArtistType
import co.edu.uniandes.misw4203.vinilos.databinding.FragmentArtistDetailBinding
import co.edu.uniandes.misw4203.vinilos.databinding.ItemArtistAlbumBinding
import co.edu.uniandes.misw4203.vinilos.ui.albums.detail.AlbumDetailViewModel
import co.edu.uniandes.misw4203.vinilos.ui.artists.labelRes
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import kotlinx.coroutines.launch

/**
 * Pantalla de detalle de un artista (HU04).
 */
class ArtistDetailFragment : Fragment() {

    private var _binding: FragmentArtistDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ArtistDetailViewModel by viewModels { ArtistDetailViewModel.Factory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArtistDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.retryButton.setOnClickListener { viewModel.loadArtistDetail() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: ArtistDetailUiState) {
        binding.loadingIndicator.isVisible = state is ArtistDetailUiState.Loading
        binding.detailContent.isVisible = state is ArtistDetailUiState.Success
        binding.errorContainer.isVisible = state is ArtistDetailUiState.Error

        when (state) {
            is ArtistDetailUiState.Success -> showArtist(state.artistDetail)
            is ArtistDetailUiState.Error ->
                binding.errorText.text =
                    state.message.ifBlank { getString(R.string.artists_error_default) }
            ArtistDetailUiState.Loading -> Unit
        }
    }

    private fun showArtist(artist: ArtistDetail) {
        val unknown = getString(R.string.album_value_unknown)

        binding.artistName.text = artist.name
        binding.artistType.text = getString(artist.type.labelRes)
        binding.artistImage.contentDescription = getString(R.string.artist_image_description, artist.name)

        Glide.with(this)
            .load(artist.image.ifBlank { null })
            .placeholder(R.drawable.ic_artist)
            .error(R.drawable.ic_artist)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .into(binding.artistImage)

        // Fecha de nacimiento o fecha de creación según el tipo de artista
        binding.dateLabel.setText(
            if (artist.type == ArtistType.MUSICIAN) R.string.artist_birth_date else R.string.artist_creation_date
        )
        val months = resources.getStringArray(R.array.months_short).toList()
        binding.dateValue.text = artist.date?.let { ArtistDetailFormatter.formatDate(it, months) } ?: unknown

        binding.artistDescription.text = artist.description
            .ifBlank { getString(R.string.artist_no_description) }

        showAlbums(artist.albums)
    }

    private fun showAlbums(albums: List<Album>) {
        binding.albumsCount.text =
            resources.getQuantityString(R.plurals.artist_albums_count, albums.size, albums.size)
        binding.albumsContainer.isVisible = albums.isNotEmpty()
        binding.albumsEmpty.isVisible = albums.isEmpty()

        binding.albumsContainer.removeAllViews()
        albums.forEach { album ->
            val row = ItemArtistAlbumBinding.inflate(layoutInflater, binding.albumsContainer, true)
            row.albumName.text = album.name
            val genreAndYear = listOf(album.genre, album.releaseDate?.year?.toString())
                .filter { !it.isNullOrBlank() }
                .joinToString(" • ")
            row.albumGenreAndYear.text = genreAndYear.ifBlank { getString(R.string.album_value_unknown) }

            Glide.with(row.albumCover)
                .load(album.cover.ifBlank { null })
                .placeholder(R.drawable.ic_album)
                .error(R.drawable.ic_album)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(row.albumCover)

            row.albumCard.setOnClickListener {
                openAlbumDetail(album.id)
            }
        }
    }

    private fun openAlbumDetail(albumId: Int) {
        findNavController().navigate(
            R.id.action_artistDetail_to_albumDetail,
            bundleOf(AlbumDetailViewModel.ARG_ALBUM_ID to albumId)
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
