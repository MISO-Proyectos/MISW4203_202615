package co.edu.uniandes.misw4203.vinilos.ui.collectors

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
import androidx.recyclerview.widget.ConcatAdapter
import co.edu.uniandes.misw4203.vinilos.R
import co.edu.uniandes.misw4203.vinilos.data.model.Collector
import co.edu.uniandes.misw4203.vinilos.databinding.FragmentCollectorListBinding
import co.edu.uniandes.misw4203.vinilos.ui.collectors.detail.CollectorDetailViewModel
import kotlinx.coroutines.launch

/**
 * Pantalla del listado de coleccionistas (HU05). Solo dibuja el estado que entrega
 * CollectorListViewModel y le avisa cuando el usuario toca "Reintentar".
 * Es igual para visitantes y coleccionistas: no depende del rol.
 */
class CollectorListFragment : Fragment() {

    private var _binding: FragmentCollectorListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CollectorListViewModel by viewModels { CollectorListViewModel.Factory }

    private val countAdapter = CollectorCountAdapter()
    private val collectorAdapter = CollectorAdapter(::openCollector)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCollectorListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.collectorList.adapter = ConcatAdapter(countAdapter, collectorAdapter)
        binding.retryButton.setOnClickListener { viewModel.loadCollectors(forceRefresh = true) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    /** HU06: abre el detalle del coleccionista tocado. */
    private fun openCollector(collector: Collector) {
        findNavController().navigate(
            R.id.action_collectorList_to_collectorDetail,
            bundleOf(CollectorDetailViewModel.ARG_COLLECTOR_ID to collector.id)
        )
    }

    private fun render(state: CollectorListUiState) {
        binding.loadingIndicator.isVisible = state is CollectorListUiState.Loading
        binding.collectorList.isVisible = state is CollectorListUiState.Success
        binding.messageContainer.isVisible =
            state is CollectorListUiState.Empty || state is CollectorListUiState.Error
        binding.retryButton.isVisible = state is CollectorListUiState.Error

        when (state) {
            is CollectorListUiState.Success -> {
                countAdapter.count = state.collectors.size
                collectorAdapter.submitList(state.collectors)
            }
            CollectorListUiState.Empty -> {
                binding.messageIcon.setImageResource(R.drawable.ic_inbox)
                binding.messageTitle.setText(R.string.collectors_empty_title)
                binding.messageText.setText(R.string.collectors_empty_subtitle)
            }
            is CollectorListUiState.Error -> {
                binding.messageIcon.setImageResource(R.drawable.ic_cloud_off)
                binding.messageTitle.setText(R.string.collectors_error_title)
                binding.messageText.text =
                    state.message.ifBlank { getString(R.string.collectors_error_default) }
            }
            CollectorListUiState.Loading -> Unit
        }
    }

    override fun onDestroyView() {
        binding.collectorList.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
