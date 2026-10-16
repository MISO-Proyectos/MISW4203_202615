package co.edu.uniandes.misw4203.vinilos.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import co.edu.uniandes.misw4203.vinilos.R
import co.edu.uniandes.misw4203.vinilos.VinilosApplication
import co.edu.uniandes.misw4203.vinilos.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ConnectionViewModel by viewModels {
        val application = requireActivity().application as VinilosApplication
        ConnectionViewModelFactory(
            application.appContainer.connectionRepository
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.retryButton.setOnClickListener {
            viewModel.checkConnection()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: ConnectionUiState) {
        when (state) {
            ConnectionUiState.Loading -> {
                binding.progressIndicator.isVisible = true
                binding.retryButton.isVisible = false
                binding.statusText.setText(R.string.backend_checking)
            }

            ConnectionUiState.Connected -> {
                binding.progressIndicator.isVisible = false
                binding.retryButton.isVisible = false
                binding.statusText.setText(R.string.backend_connected)
            }

            is ConnectionUiState.Error -> {
                binding.progressIndicator.isVisible = false
                binding.retryButton.isVisible = true
                binding.statusText.text = getString(
                    R.string.backend_error
                ) + ": " + state.message
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}