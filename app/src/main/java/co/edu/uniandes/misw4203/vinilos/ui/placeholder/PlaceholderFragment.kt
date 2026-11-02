package co.edu.uniandes.misw4203.vinilos.ui.placeholder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import co.edu.uniandes.misw4203.vinilos.R
import co.edu.uniandes.misw4203.vinilos.databinding.FragmentPlaceholderBinding

/**
 * Pantalla provisional para las secciones que todavía no existen
 * (Artistas y Coleccionistas se hacen en el Sprint 2).
 * El mensaje y el ícono llegan como argumentos desde nav_graph.xml.
 */
class PlaceholderFragment : Fragment() {

    private var _binding: FragmentPlaceholderBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaceholderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = arguments
        val message = args?.getInt(ARG_MESSAGE, 0)?.takeIf { it != 0 }
            ?: R.string.placeholder_next_sprint
        val icon = args?.getInt(ARG_ICON, 0)?.takeIf { it != 0 }
            ?: R.drawable.ic_album

        binding.placeholderMessage.setText(message)
        binding.placeholderIcon.setImageResource(icon)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_MESSAGE = "message"
        const val ARG_ICON = "icon"
    }
}
