package co.edu.uniandes.misw4203.vinilos.ui.role

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import co.edu.uniandes.misw4203.vinilos.R
import co.edu.uniandes.misw4203.vinilos.databinding.FragmentRoleSelectionBinding

/** Primera pantalla de la app: el usuario elige si entra como visitante o coleccionista. */
class RoleSelectionFragment : Fragment() {

    private var _binding: FragmentRoleSelectionBinding? = null
    private val binding get() = _binding!!

    private val session: SessionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoleSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.visitorCard.setOnClickListener { chooseRole(UserRole.VISITOR) }
        binding.collectorCard.setOnClickListener { chooseRole(UserRole.COLLECTOR) }
    }

    private fun chooseRole(role: UserRole) {
        session.selectRole(role)
        findNavController().navigate(R.id.action_roleSelection_to_albumList)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
