package co.edu.uniandes.misw4203.vinilos.ui.albums.create

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import co.edu.uniandes.misw4203.vinilos.R
import co.edu.uniandes.misw4203.vinilos.data.model.AlbumGenre
import co.edu.uniandes.misw4203.vinilos.data.model.RecordLabel
import co.edu.uniandes.misw4203.vinilos.databinding.FragmentCreateAlbumBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

/**
 * Pantalla "Agregar álbum" (HU07). Le pasa al ViewModel lo que escribe o elige el usuario,
 * dibuja los errores del formulario y, cuando el álbum se crea, vuelve al catálogo
 * dejando el nombre en [RESULT_CREATED_ALBUM] para que el catálogo muestre el aviso.
 */
class CreateAlbumFragment : Fragment() {

    private var _binding: FragmentCreateAlbumBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreateAlbumViewModel by viewModels { CreateAlbumViewModel.Factory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateAlbumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpTextFields()
        setUpDropdowns()
        setUpDatePicker()

        binding.saveButton.setOnClickListener {
            hideKeyboard()
            viewModel.save()
        }
        binding.cancelButton.setOnClickListener { findNavController().navigateUp() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.events.collect(::handleEvent) }
            }
        }
    }

    private fun setUpTextFields() {
        binding.nameInput.doAfterTextChanged { viewModel.onNameChanged(it?.toString().orEmpty()) }
        binding.coverInput.doAfterTextChanged { viewModel.onCoverChanged(it?.toString().orEmpty()) }
        binding.descriptionInput.doAfterTextChanged {
            viewModel.onDescriptionChanged(it?.toString().orEmpty())
        }
    }

    /**
     * Género y sello: se muestran los textos en español, pero al ViewModel se le pasa
     * la opción (enum) según la posición elegida.
     */
    private fun setUpDropdowns() {
        // El texto lo pone render() desde el estado; si la vista lo restaurara sola,
        // la lista quedaría filtrada y solo mostraría la opción ya elegida
        binding.genreInput.isSaveEnabled = false
        binding.recordLabelInput.isSaveEnabled = false

        binding.genreInput.setSimpleItems(AlbumGenre.entries.map { getString(it.labelRes) }.toTypedArray())
        binding.genreInput.setOnItemClickListener { _, _, position, _ ->
            viewModel.onGenreSelected(AlbumGenre.entries[position])
        }

        binding.recordLabelInput.setSimpleItems(RecordLabel.entries.map { it.apiValue }.toTypedArray())
        binding.recordLabelInput.setOnItemClickListener { _, _, position, _ ->
            viewModel.onRecordLabelSelected(RecordLabel.entries[position])
        }
    }

    /** La fecha no se escribe: tocar el campo o el ícono abre el calendario. */
    private fun setUpDatePicker() {
        binding.releaseDateInput.isSaveEnabled = false
        binding.releaseDateInput.setOnClickListener { openDatePicker() }
        binding.releaseDateLayout.setEndIconOnClickListener { openDatePicker() }

        // Si el calendario quedó abierto al girar el teléfono, se vuelve a escuchar su resultado
        @Suppress("UNCHECKED_CAST")
        (childFragmentManager.findFragmentByTag(DATE_PICKER_TAG) as? MaterialDatePicker<Long>)
            ?.let(::listenToDatePicker)
    }

    private fun openDatePicker() {
        if (childFragmentManager.findFragmentByTag(DATE_PICKER_TAG) != null) return
        val today = MaterialDatePicker.todayInUtcMilliseconds()
        val selection = viewModel.uiState.value.releaseDate
            ?.let(CreateAlbumFormatter::toUtcMillis) ?: today

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.create_album_release_date_label)
            .setSelection(selection)
            .setCalendarConstraints(
                // No se pueden elegir fechas futuras
                CalendarConstraints.Builder()
                    .setEnd(today)
                    .setValidator(DateValidatorPointBackward.now())
                    .build()
            )
            .build()
        listenToDatePicker(picker)
        picker.show(childFragmentManager, DATE_PICKER_TAG)
    }

    private fun listenToDatePicker(picker: MaterialDatePicker<Long>) {
        picker.addOnPositiveButtonClickListener { millis ->
            viewModel.onReleaseDateSelected(CreateAlbumFormatter.fromUtcMillis(millis))
        }
    }

    private fun render(state: CreateAlbumUiState) {
        binding.releaseDateInput.setText(
            state.releaseDate?.let(CreateAlbumFormatter::formatDate).orEmpty()
        )
        binding.genreInput.showSelection(state.genre?.let { getString(it.labelRes) })
        binding.recordLabelInput.showSelection(state.recordLabel?.apiValue)

        showError(binding.nameLayout, AlbumFormField.NAME, state.errors)
        showError(binding.coverLayout, AlbumFormField.COVER, state.errors)
        showError(binding.releaseDateLayout, AlbumFormField.RELEASE_DATE, state.errors)
        showError(binding.genreLayout, AlbumFormField.GENRE, state.errors)
        showError(binding.recordLabelLayout, AlbumFormField.RECORD_LABEL, state.errors)
        showError(binding.descriptionLayout, AlbumFormField.DESCRIPTION, state.errors)

        val enabled = !state.isSaving
        listOf(
            binding.nameLayout, binding.coverLayout, binding.releaseDateLayout,
            binding.genreLayout, binding.recordLabelLayout, binding.descriptionLayout
        ).forEach { it.isEnabled = enabled }
        binding.saveButton.isEnabled = enabled
        binding.cancelButton.isEnabled = enabled
        binding.savingIndicator.isVisible = state.isSaving
    }

    /** Pone el texto de la opción elegida sin filtrar la lista desplegable. */
    private fun MaterialAutoCompleteTextView.showSelection(label: String?) {
        val value = label.orEmpty()
        if (text.toString() != value) setText(value, false)
    }

    /**
     * El error de "obligatorio" nombra el campo, para que TalkBack no lea cinco
     * mensajes iguales sin decir a cuál campo pertenece cada uno.
     */
    private fun showError(
        layout: TextInputLayout,
        field: AlbumFormField,
        errors: Map<AlbumFormField, AlbumFormError>
    ) {
        val message = errors[field]?.let {
            getString(if (it == AlbumFormError.REQUIRED) field.requiredRes else it.messageRes)
        }
        if (layout.error?.toString() != message) layout.error = message
    }

    private fun handleEvent(event: CreateAlbumEvent) {
        when (event) {
            is CreateAlbumEvent.Created -> {
                val navController = findNavController()
                navController.previousBackStackEntry?.savedStateHandle
                    ?.set(RESULT_CREATED_ALBUM, event.albumName)
                navController.popBackStack()
            }
            is CreateAlbumEvent.Failed ->
                Snackbar.make(
                    binding.root,
                    event.message.ifBlank { getString(R.string.create_album_error_default) },
                    Snackbar.LENGTH_LONG
                ).show()
        }
    }

    private fun hideKeyboard() {
        val focused = activity?.currentFocus ?: return
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(focused.windowToken, 0)
        focused.clearFocus()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        /** Clave con la que el catálogo recibe el nombre del álbum recién creado. */
        const val RESULT_CREATED_ALBUM = "created_album_name"

        private const val DATE_PICKER_TAG = "release_date_picker"

        @get:StringRes
        private val AlbumGenre.labelRes: Int
            get() = when (this) {
                AlbumGenre.CLASSICAL -> R.string.genre_classical
                AlbumGenre.SALSA -> R.string.genre_salsa
                AlbumGenre.ROCK -> R.string.genre_rock
                AlbumGenre.FOLK -> R.string.genre_folk
            }

        @get:StringRes
        private val AlbumFormField.requiredRes: Int
            get() = when (this) {
                AlbumFormField.NAME -> R.string.create_album_error_required_name
                AlbumFormField.COVER -> R.string.create_album_error_required_cover
                AlbumFormField.RELEASE_DATE -> R.string.create_album_error_required_release_date
                AlbumFormField.GENRE -> R.string.create_album_error_required_genre
                AlbumFormField.RECORD_LABEL -> R.string.create_album_error_required_record_label
                AlbumFormField.DESCRIPTION -> R.string.create_album_error_required_description
            }

        @get:StringRes
        private val AlbumFormError.messageRes: Int
            get() = when (this) {
                AlbumFormError.REQUIRED -> R.string.create_album_error_required
                AlbumFormError.INVALID_URL -> R.string.create_album_error_url
                AlbumFormError.FUTURE_DATE -> R.string.create_album_error_future_date
            }
    }
}
