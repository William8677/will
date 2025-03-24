package com.williamfq.xhat

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.firebase.auth.FirebaseAuth
import com.williamfq.domain.location.LocationTracker
import com.williamfq.xhat.network.NetworkManager
import com.williamfq.xhat.ui.Navigation.AppNavigation
import com.williamfq.xhat.ui.Navigation.NavigationState
import com.williamfq.xhat.ui.Navigation.Screen
import com.williamfq.xhat.ui.theme.XhatTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var auth: FirebaseAuth
    @Inject lateinit var locationTracker: LocationTracker
    private lateinit var permissionsViewModel: PermissionsViewModel
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var navigationState: NavigationState
    private var permissionRequestCount = 0
    private val maxPermissionRequests = 3

    companion object {
        private const val PERMISSIONS_GRANTED_TIME = "permissions_granted_time"
        private const val PROFILE_SETUP_COMPLETE = "profile_setup_complete"
        private const val TEST_DEVICE_ID = "TEST-DEVICE-ID"
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsViewModel.updatePermissionsStatus(permissions)
        permissionRequestCount++
        if (areAllPermissionsGranted()) {
            onAllPermissionsGranted()
        } else {
            showPermissionRequiredDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionsViewModel = ViewModelProvider(this)[PermissionsViewModel::class.java]
        navigationState = NavigationState()
        sharedPreferences = getSharedPreferences("xhat_preferences", MODE_PRIVATE)

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build())
        }

        permissionsViewModel.initializePermissionsLauncher(permissionLauncher::launch)

        lifecycleScope.launch(Dispatchers.IO) {
            createWebViewCacheDirInBackground()
            withContext(Dispatchers.Main) {
                initializeApp()
            }
        }

        setContent {
            XhatTheme {
                val navController = rememberNavController()
                val startDestination = calculateStartDestination()
                AppNavigation(
                    navController = navController,
                    startDestination = startDestination,
                    onRequestPermissions = { permissionsViewModel.requestPermissionsIfNeeded() },
                    permissionsGranted = areAllPermissionsGranted(),
                    navigationState = navigationState,
                    locationTracker = locationTracker
                )
            }
        }
    }

    private fun calculateStartDestination(): String {
        return if (auth.currentUser != null) {
            if (sharedPreferences.getBoolean(PROFILE_SETUP_COMPLETE, false)) {
                Screen.Chats.route
            } else {
                Screen.ProfileSetup.route
            }
        } else {
            Screen.PhoneNumber.route
        }
    }

    private suspend fun createWebViewCacheDirInBackground() = withContext(Dispatchers.IO) {
        val cacheDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/js")
        if (!cacheDir.exists()) {
            try {
                if (cacheDir.mkdirs()) {
                    Timber.d("Directorio de caché WebView creado: ${cacheDir.absolutePath}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error creando directorio de caché WebView")
            }
        }
    }

    private fun initializeAdMob() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (BuildConfig.DEBUG) {
                    val configuration = RequestConfiguration.Builder()
                        .setTestDeviceIds(listOf(TEST_DEVICE_ID))
                        .build()
                    MobileAds.setRequestConfiguration(configuration)
                }
                withContext(Dispatchers.Main) {
                    MobileAds.initialize(this@MainActivity) { initializationStatus ->
                        initializationStatus.adapterStatusMap.forEach { (adapter, status) ->
                            Timber.d("AdMob Adapter $adapter: ${status.description} (Latencia: ${status.latency}ms)")
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error en la inicialización de AdMob")
            }
        }
    }

    private fun initializeApp() {
        if (!areAllPermissionsGranted()) {
            permissionsViewModel.requestPermissionsIfNeeded()
        } else {
            onAllPermissionsGranted()
        }
    }

    private fun areAllPermissionsGranted(): Boolean =
        permissionsViewModel.requiredPermissions.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    private fun onAllPermissionsGranted() {
        sharedPreferences.edit { putLong(PERMISSIONS_GRANTED_TIME, System.currentTimeMillis()) }
        lifecycleScope.launch(Dispatchers.IO) {
            initializeAdMob()
        }
    }

    private fun showPermissionRequiredDialog() {
        val deniedPermissions = permissionsViewModel.permissionsStatus.value.filter { !it.value }.keys
        val message = if (deniedPermissions.isNotEmpty()) {
            "Los siguientes permisos son necesarios pero no han sido concedidos: ${deniedPermissions.joinToString()}"
        } else {
            "Xhat necesita permisos para ofrecerte la mejor experiencia. Por favor, concédelos."
        }
        AlertDialog.Builder(this)
            .setTitle("Permisos Requeridos")
            .setMessage(message)
            .setPositiveButton("Reintentar") { _, _ -> permissionsViewModel.requestPermissionsIfNeeded() }
            .setNegativeButton("Continuar sin permisos") { _, _ ->
                Timber.w("Usuario decidió continuar sin permisos")
            }
            .setCancelable(false)
            .show()
    }

    private fun showPermissionSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permisos Necesarios")
            .setMessage("Habilita los permisos en la configuración para disfrutar de todas las funciones de Xhat.")
            .setPositiveButton("Ir a Configuración") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", packageName, null)
                startActivity(intent)
            }
            .setNegativeButton("Cancelar") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (permissionRequestCount >= maxPermissionRequests) {
            showPermissionSettingsDialog()
        } else if (!areAllPermissionsGranted()) {
            showPermissionRequiredDialog()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        permissionsViewModel.cleanup()
        lifecycleScope.cancel()
    }
    fun sendTestMessage() {
        lifecycleScope.launch {
            try {
                val success = NetworkManager.sendMessageWithPreview("Hola, mundo!", "recipient123")
                if (success) {
                    Timber.d("Mensaje enviado con éxito")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error al enviar mensaje")
            }
        }
    }
}