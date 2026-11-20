package co.edu.uniandes.misw4203.vinilos.ui.albums.detail

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.TextView
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.os.bundleOf
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
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
import co.edu.uniandes.misw4203.vinilos.data.model.Comment
import co.edu.uniandes.misw4203.vinilos.data.model.Track
import co.edu.uniandes.misw4203.vinilos.databinding.FragmentAlbumDetailBinding
import co.edu.uniandes.misw4203.vinilos.databinding.ItemCommentBinding
import co.edu.uniandes.misw4203.vinilos.databinding.ItemTrackBinding
import co.edu.uniandes.misw4203.vinilos.ui.albums.collection.AddToCollectionFragment
import co.edu.uniandes.misw4203.vinilos.ui.albums.collection.AddToCollectionViewModel
import co.edu.uniandes.misw4203.vinilos.ui.albums.comments.AddCommentFragment
import co.edu.uniandes.misw4203.vinilos.ui.albums.comments.AddCommentViewModel
import co.edu.uniandes.misw4203.vinilos.ui.albums.tracks.AddTrackFragment
import co.edu.uniandes.misw4203.vinilos.ui.albums.tracks.AddTrackViewModel
import co.edu.uniandes.misw4203.vinilos.ui.role.SessionViewModel
import co.edu.uniandes.misw4203.vinilos.ui.role.UserRole
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Pantalla de detalle de un álbum (HU02 / HU08 / HU09 / HU11). Solo dibuja el estado que entrega
 * AlbumDetailViewModel; los botones "Agregar track", "Comentar" y "Agregar a colección" dependen del rol de SessionViewModel.
 * "Agregar track" abre el formulario para asociar una canción (HU08).
 * Las listas de tracks y comentarios son cortas y van dentro del scroll, por eso
 * se dibujan como filas en un LinearLayout en vez de un RecyclerView.
 */
class AlbumDetailFragment : Fragment() {

    private var _binding: FragmentAlbumDetailBinding? = null
    private val binding get() = _binding!!
    private var snackbar: Snackbar? = null

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
            findNavController().navigate(
                R.id.action_albumDetail_to_addTrack,
                bundleOf(AddTrackViewModel.ARG_ALBUM_ID to viewModel.currentAlbumId)
            )
        }
        binding.addToCollectionButton.setOnClickListener {
            findNavController().navigate(
                R.id.action_albumDetail_to_addToCollection,
                bundleOf(AddToCollectionViewModel.ARG_ALBUM_ID to viewModel.currentAlbumId)
            )
        }
        observeAddedTrack()
        observeAddedToCollection()
        observeAddedComment()
        binding.addCommentButton.setOnClickListener {
            findNavController().navigate(
                R.id.action_albumDetail_to_addComment,
                bundleOf(AddCommentViewModel.ARG_ALBUM_ID to viewModel.currentAlbumId)
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { session.role.collect(::renderRole) }
            }
        }
    }

    /**
     * HU08: al volver del formulario con una canción agregada, se recarga el álbum,
     * se resalta la canción y se muestra el aviso con su nombre.
     */
    private fun observeAddedTrack() {
        val savedStateHandle = findNavController().currentBackStackEntry?.savedStateHandle ?: return
        savedStateHandle.getLiveData<Bundle>(AddTrackFragment.RESULT_ADDED_TRACK)
            .observe(viewLifecycleOwner) { result ->
                if (result == null) return@observe
                savedStateHandle.remove<Bundle>(AddTrackFragment.RESULT_ADDED_TRACK)
                viewModel.onTrackAdded(result.getInt(AddTrackFragment.RESULT_TRACK_ID))
                showMessage(
                    getString(
                        R.string.add_track_success,
                        result.getString(AddTrackFragment.RESULT_TRACK_NAME).orEmpty()
                    )
                )
            }
    }

    /**
     * HU09: al volver del formulario con un comentario publicado, se recarga el álbum
     * para que aparezca el comentario nuevo y se muestra el aviso.
     */
    private fun observeAddedComment() {
        val savedStateHandle = findNavController().currentBackStackEntry?.savedStateHandle ?: return
        savedStateHandle.getLiveData<Boolean>(AddCommentFragment.RESULT_ADDED_COMMENT)
            .observe(viewLifecycleOwner) { added ->
                if (added != true) return@observe
                savedStateHandle.remove<Boolean>(AddCommentFragment.RESULT_ADDED_COMMENT)
                viewModel.loadAlbum()
                showMessage(getString(R.string.add_comment_success))
            }
    }

    /**
     * HU11: al volver del formulario con el álbum agregado, se muestra el aviso con el nombre
     * del álbum y del coleccionista. El detalle no se recarga: la colección se ve en HU06.
     */
    private fun observeAddedToCollection() {
        val savedStateHandle = findNavController().currentBackStackEntry?.savedStateHandle ?: return
        savedStateHandle.getLiveData<Bundle>(AddToCollectionFragment.RESULT_ADDED_TO_COLLECTION)
            .observe(viewLifecycleOwner) { result ->
                if (result == null) return@observe
                savedStateHandle.remove<Bundle>(AddToCollectionFragment.RESULT_ADDED_TO_COLLECTION)
                showMessage(
                    getString(
                        R.string.add_to_collection_success,
                        result.getString(AddToCollectionFragment.RESULT_ALBUM_NAME).orEmpty(),
                        result.getString(AddToCollectionFragment.RESULT_COLLECTOR_NAME).orEmpty()
                    )
                )
            }
    }

    private fun renderRole(role: UserRole?) {
        binding.addTrackButton.isVisible = role?.canAddTracks == true
        binding.addCommentButton.isVisible = role?.canAddComments == true
        binding.addToCollectionButton.isVisible = role?.canAddToCollection == true
    }

    private fun render(state: AlbumDetailUiState) {
        binding.loadingIndicator.isVisible = state is AlbumDetailUiState.Loading
        binding.detailContent.isVisible = state is AlbumDetailUiState.Success
        binding.errorContainer.isVisible = state is AlbumDetailUiState.Error

        when (state) {
            is AlbumDetailUiState.Success -> showAlbum(state.album, state.highlightedTrackId)
            is AlbumDetailUiState.Error ->
                binding.errorText.text =
                    state.message.ifBlank { getString(R.string.albums_error_default) }
            AlbumDetailUiState.Loading -> Unit
        }
    }

    private fun showAlbum(album: Album, highlightedTrackId: Int?) {
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

        showTracks(album.tracks, highlightedTrackId)
        showComments(album.comments)
    }

    private fun showTracks(tracks: List<Track>, highlightedTrackId: Int?) {
        binding.tracksCount.text =
            resources.getQuantityString(R.plurals.album_tracks_count, tracks.size, tracks.size)
        binding.tracksContainer.isVisible = tracks.isNotEmpty()
        binding.tracksEmpty.isVisible = tracks.isEmpty()

        binding.tracksContainer.removeAllViews()
        // La canción recién agregada (HU08) se pinta en naranja, como en el prototipo
        val highlightColor = MaterialColors.getColor(binding.tracksContainer, androidx.appcompat.R.attr.colorPrimary)
        tracks.forEachIndexed { index, track ->
            val row = ItemTrackBinding.inflate(layoutInflater, binding.tracksContainer, true)
            val duration = AlbumDetailFormatter.formatDuration(track.duration)
            row.trackDivider.isVisible = index > 0
            row.trackNumber.text = AlbumDetailFormatter.formatTrackNumber(index + 1)
            row.trackName.text = track.name
            row.trackDuration.text = duration
            if (track.id == highlightedTrackId) {
                row.trackName.setTextColor(highlightColor)
                row.trackDuration.setTextColor(highlightColor)
            }
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

    /**
     * Muestra un aviso de esta pantalla. Material lo agrega al contenido de la actividad, no al
     * fragmento, y sobreviviría a la navegación: al volver al catálogo taparía la barra inferior.
     * Por eso se guarda y se cierra al destruir la vista. (En el detalle la barra inferior no
     * está visible, así que no hay a qué anclarlo con setAnchorView.)
     *
     * Con un lector de pantalla (TalkBack) el aviso de 2,75 s de `LENGTH_LONG` se iba antes de
     * terminar de leerse y el foco saltaba a «Volver»: se le da un tiempo acorde al texto y se
     * lleva el foco de accesibilidad al aviso en cuanto aparece (A11Y-29). Sin lector de pantalla
     * queda igual que antes.
     */
    private fun showMessage(message: String) {
        snackbar?.dismiss()
        val bar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        if (isScreenReaderActive()) {
            configureForScreenReader(bar, message)
        }
        snackbar = bar.also { it.show() }
    }

    private fun isScreenReaderActive(): Boolean {
        val manager = requireContext().getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        return manager != null && manager.isEnabled && manager.isTouchExplorationEnabled
    }

    // Lint avisa de que no conviene forzar el foco de accesibilidad. Aquí es lo buscado: sin esto TalkBack
    // deja el foco en «Volver» y el aviso de éxito no se lee (A11Y-29). Solo se hace con lector activo.
    @SuppressLint("AccessibilityFocus")
    private fun configureForScreenReader(bar: Snackbar, message: String) {
        val manager = requireContext().getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val base = SCREEN_READER_BASE_MS + SCREEN_READER_MS_PER_CHAR * message.length
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Material vuelve a pasar el valor por getRecommendedTimeoutMillis, que solo lo sube
            // si el usuario configuró un tiempo mayor: no se acumula.
            bar.duration = manager.getRecommendedTimeoutMillis(
                base,
                AccessibilityManager.FLAG_CONTENT_TEXT or AccessibilityManager.FLAG_CONTENT_CONTROLS
            )
        } else {
            bar.duration = Snackbar.LENGTH_INDEFINITE
            bar.setAction(R.string.a11y_snackbar_close) { bar.dismiss() }
        }
        bar.addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
            override fun onShown(transientBottomBar: Snackbar) {
                transientBottomBar.view
                    .findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                    ?.performAccessibilityAction(AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS, null)
            }
        })
    }

    override fun onDestroyView() {
        snackbar?.dismiss()
        snackbar = null
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

        /** Tiempo del aviso con lector de pantalla: 4 s fijos más 80 ms por carácter (≈ 10 s para 75 caracteres). */
        const val SCREEN_READER_BASE_MS = 4_000
        const val SCREEN_READER_MS_PER_CHAR = 80
    }
}
