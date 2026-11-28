package co.edu.uniandes.misw4203.vinilos.ui.artists.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
import co.edu.uniandes.misw4203.vinilos.ui.artists.favorite.AddFavoriteArtistFragment
import co.edu.uniandes.misw4203.vinilos.ui.artists.favorite.AddFavoriteArtistViewModel
import co.edu.uniandes.misw4203.vinilos.ui.artists.labelRes
import co.edu.uniandes.misw4203.vinilos.ui.common.navigateOnce
import co.edu.uniandes.misw4203.vinilos.ui.role.SessionViewModel
import co.edu.uniandes.misw4203.vinilos.ui.role.UserRole
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Pantalla de detalle de un artista (HU04 / HU10). El botón "Agregar a favoritos" depende del
 * rol de SessionViewModel y abre el formulario de HU10 con los datos del artista.
 */
class ArtistDetailFragment : Fragment() {

    private var _binding: FragmentArtistDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ArtistDetailViewModel by viewModels { ArtistDetailViewModel.Factory }
    private val session: SessionViewModel by activityViewModels()

    /** Artista que muestra la pantalla; el formulario de HU10 recibe sus datos como argumentos. */
    private var currentArtist: ArtistDetail? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentArtistDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.retryButton.setOnClickListener { viewModel.loadArtistDetail() }
        binding.addFavoriteButton.setOnClickListener { openAddFavorite() }
        observeAddedFavorite()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { session.role.collect(::renderRole) }
            }
        }
    }

    /**
     * HU10: al volver del formulario con el favorito agregado, se muestra el aviso con el nombre
     * del artista y del coleccionista. El detalle no se recarga: el favorito se ve en HU06.
     */
    private fun observeAddedFavorite() {
        val savedStateHandle = findNavController().currentBackStackEntry?.savedStateHandle ?: return
        savedStateHandle.getLiveData<Bundle>(AddFavoriteArtistFragment.RESULT_ADDED_FAVORITE)
            .observe(viewLifecycleOwner) { result ->
                if (result == null) return@observe
                savedStateHandle.remove<Bundle>(AddFavoriteArtistFragment.RESULT_ADDED_FAVORITE)
                Snackbar.make(
                    binding.root,
                    getString(
                        R.string.add_favorite_success,
                        result.getString(AddFavoriteArtistFragment.RESULT_ARTIST_NAME).orEmpty(),
                        result.getString(AddFavoriteArtistFragment.RESULT_COLLECTOR_NAME).orEmpty()
                    ),
                    Snackbar.LENGTH_LONG
                ).show()
            }
    }

    private fun renderRole(role: UserRole?) {
        binding.addFavoriteButton.isVisible = role?.canAddFavorites == true
    }

    private fun openAddFavorite() {
        val artist = currentArtist ?: return
        findNavController().navigateOnce(
            R.id.action_artistDetail_to_addFavorite,
            bundleOf(
                AddFavoriteArtistViewModel.ARG_ARTIST_ID to artist.id,
                AddFavoriteArtistViewModel.ARG_ARTIST_TYPE to artist.type.name,
                AddFavoriteArtistViewModel.ARG_ARTIST_NAME to artist.name,
                AddFavoriteArtistViewModel.ARG_ALBUM_COUNT to artist.albums.size
            )
        )
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
        currentArtist = artist
        val unknown = getString(R.string.album_value_unknown)

        binding.artistName.text = artist.name
        binding.artistType.text = getString(artist.type.labelRes)
        binding.artistImage.contentDescription = getString(R.string.artist_image_description, artist.name)

        val imageSize = resources.getDimensionPixelSize(R.dimen.artist_detail_image_size)
        Glide.with(binding.artistImage)
            .load(artist.image.ifBlank { null })
            .placeholder(R.drawable.ic_artist)
            .error(R.drawable.ic_artist)
            .override(imageSize)
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

        val coverSize = resources.getDimensionPixelSize(R.dimen.artist_album_cover_size)
        binding.albumsContainer.removeAllViews()
        albums.forEach { album ->
            val row = ItemArtistAlbumBinding.inflate(layoutInflater, binding.albumsContainer, true)
            row.albumName.text = album.name
            val genreAndYear = listOf(album.genre, album.releaseDate?.year?.toString())
                .filterNot { it.isNullOrBlank() }
                .joinToString(" • ")
            row.albumGenreAndYear.text = genreAndYear.ifBlank { getString(R.string.album_value_unknown) }

            Glide.with(row.albumCover)
                .load(album.cover.ifBlank { null })
                .placeholder(R.drawable.ic_album)
                .error(R.drawable.ic_album)
                .override(coverSize)
                .format(DecodeFormat.PREFER_RGB_565)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(row.albumCover)

            row.albumCard.setOnClickListener {
                openAlbumDetail(album.id)
            }
        }
    }

    private fun openAlbumDetail(albumId: Int) {
        findNavController().navigateOnce(
            R.id.action_artistDetail_to_albumDetail,
            bundleOf(AlbumDetailViewModel.ARG_ALBUM_ID to albumId)
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
