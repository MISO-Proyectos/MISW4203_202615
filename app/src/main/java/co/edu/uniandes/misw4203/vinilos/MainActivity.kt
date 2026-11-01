package co.edu.uniandes.misw4203.vinilos

import android.content.res.ColorStateList
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import co.edu.uniandes.misw4203.vinilos.databinding.ActivityMainBinding
import co.edu.uniandes.misw4203.vinilos.ui.role.SessionViewModel
import co.edu.uniandes.misw4203.vinilos.ui.role.UserRole
import com.google.android.material.color.MaterialColors
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

    /** Rol actual y si la pantalla es una sección de la barra inferior; deciden qué se ve arriba. */
    private var currentRole: UserRole? = null
    private var onTopLevelScreen = false

    /** Android 17 exige este permiso para llegar al backend en la red local (10.0.2.2 en el emulador). */
    private val localNetworkPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

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
        requestLocalNetworkAccess()

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)
        binding.roleChip.setOnClickListener { changeRole() }
        binding.topBarIcon.setOnClickListener { navController.navigateUp() }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            // En la selección de rol no se muestran las barras.
            // En las secciones principales se ven la barra inferior y el chip del rol;
            // en las demás pantallas (por ejemplo, el detalle) aparece la flecha "volver".
            onTopLevelScreen = destination.id in TOP_LEVEL_DESTINATIONS
            binding.topBar.isVisible = destination.id != R.id.roleSelectionFragment
            binding.bottomBar.isVisible = onTopLevelScreen
            binding.topBarTitle.text = destination.label
            updateTopBarIcon()
            updateRoleChip()
            updateBottomInset()
        }

        observeRole()
    }

    /**
     * La app dibuja detrás de las barras del sistema (edge-to-edge). El espacio de la
     * barra de estado queda en la raíz y el de la barra de navegación debajo de la
     * barra inferior, para que su fondo llegue hasta el borde. Funciona igual desde API 21.
     */
    /** Pide el permiso de red local solo en Android 17 (API 37) o superior; antes no existe. */
    private fun requestLocalNetworkAccess() {
        if (Build.VERSION.SDK_INT < 37) return
        val permission = "android.permission.ACCESS_LOCAL_NETWORK"
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            localNetworkPermission.launch(permission)
        }
    }

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
        currentRole = role
        updateRoleChip()
    }

    private fun updateRoleChip() {
        val label = currentRole?.let { getString(it.labelRes) }
        binding.roleChip.isVisible = label != null && onTopLevelScreen
        binding.roleChip.text = label
        binding.roleChip.contentDescription = label?.let { getString(R.string.change_role, it) }
    }

    /** Disco naranja (decorativo) en las secciones principales; flecha "volver" en las demás. */
    private fun updateTopBarIcon() {
        val icon = binding.topBarIcon
        val back = !onTopLevelScreen
        val tintAttr = if (back) {
            com.google.android.material.R.attr.colorOnSurface
        } else {
            androidx.appcompat.R.attr.colorPrimary
        }
        icon.setImageResource(if (back) R.drawable.ic_arrow_back else R.drawable.ic_album)
        ImageViewCompat.setImageTintList(
            icon,
            ColorStateList.valueOf(MaterialColors.getColor(icon, tintAttr))
        )
        icon.isClickable = back
        icon.isFocusable = back
        icon.contentDescription = if (back) getString(R.string.navigate_up) else null
        icon.importantForAccessibility = if (back) {
            View.IMPORTANT_FOR_ACCESSIBILITY_YES
        } else {
            View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
    }

    private companion object {
        /** Secciones de la barra inferior (los id coinciden con menu/bottom_nav_menu.xml). */
        val TOP_LEVEL_DESTINATIONS = setOf(
            R.id.albumListFragment,
            R.id.artistListFragment,
            R.id.collectorListFragment
        )
    }
}
