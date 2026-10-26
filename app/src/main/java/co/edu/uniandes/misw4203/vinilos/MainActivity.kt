package co.edu.uniandes.misw4203.vinilos

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import co.edu.uniandes.misw4203.vinilos.databinding.ActivityMainBinding
import co.edu.uniandes.misw4203.vinilos.ui.role.SessionViewModel
import co.edu.uniandes.misw4203.vinilos.ui.role.UserRole
import kotlinx.coroutines.launch

/**
 * Actividad única de Vinilos. Contiene el NavHost, la barra superior (título de la
 * sección y chip del rol) y la barra de navegación inferior (Álbumes, Artistas y
 * Coleccionistas), como en el prototipo de Figma.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val session: SessionViewModel by viewModels()

    /** Alto de la barra de navegación del sistema; se recalcula con los insets. */
    private var systemBottomInset = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        // El prototipo es siempre oscuro: íconos claros en las barras del sistema
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarInsets()

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)
        binding.roleChip.setOnClickListener { changeRole() }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            // En la selección de rol no se muestran las barras
            val showBars = destination.id != R.id.roleSelectionFragment
            binding.topBar.isVisible = showBars
            binding.bottomBar.isVisible = showBars
            binding.topBarTitle.text = destination.label
            updateBottomInset()
        }

        observeRole()
    }

    /**
     * La app dibuja detrás de las barras del sistema (edge-to-edge). El espacio de la
     * barra de estado queda en la raíz y el de la barra de navegación debajo de la
     * barra inferior, para que su fondo llegue hasta el borde. Funciona igual desde API 21.
     */
    private fun applySystemBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right)
            systemBottomInset = bars.bottom
            updateBottomInset()
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun updateBottomInset() {
        val barVisible = binding.bottomBar.isVisible
        binding.bottomBar.updatePadding(bottom = if (barVisible) systemBottomInset else 0)
        binding.navHostFragment.updatePadding(bottom = if (barVisible) 0 else systemBottomInset)
    }

    /** Vuelve a la selección de rol y borra la pila de pantallas. */
    private fun changeRole() {
        session.clearRole()
        navController.navigate(R.id.action_global_roleSelection)
    }

    private fun observeRole() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                session.role.collect(::showRole)
            }
        }
    }

    private fun showRole(role: UserRole?) {
        val label = role?.let { getString(it.labelRes) }
        binding.roleChip.isVisible = role != null
        binding.roleChip.text = label
        binding.roleChip.contentDescription = label?.let { getString(R.string.change_role, it) }
    }
}
