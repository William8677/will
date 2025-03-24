package com.williamfq.xhat.ui.Navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Forum
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.williamfq.domain.location.LocationTracker
import com.williamfq.xhat.ui.main.ScaffoldWrapper
import com.williamfq.xhat.ui.screens.auth.PhoneNumberScreen
import com.williamfq.xhat.ui.screens.auth.ProfileSetupScreen
import com.williamfq.xhat.ui.screens.auth.VerificationCodeScreen
import com.williamfq.xhat.ui.screens.login.LoginScreen
import com.williamfq.xhat.ui.screens.register.RegisterScreen
import com.williamfq.xhat.ui.screens.profile.ProfileScreen
import com.williamfq.xhat.ui.screens.settings.SettingsScreen
import com.williamfq.xhat.panic.RealTimeLocationScreen
import com.williamfq.xhat.ui.screens.chat.ChatScreen
import com.williamfq.xhat.ui.stories.StoriesScreen
import com.williamfq.xhat.ui.channels.ChannelScreen
import com.williamfq.xhat.ui.communities.screens.CommunitiesScreen
import com.williamfq.xhat.ui.call.screens.CallScreen
import com.williamfq.xhat.ui.screens.main.components.EmptyScreenPlaceholder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
import java.lang.IllegalArgumentException

sealed class NavigationEvent {
    data class NavigateToChat(val chatId: String) : NavigationEvent()
    data class ActivatePanicMode(val chatId: String, val chatType: ChatType) : NavigationEvent()
    object NavigateBack : NavigationEvent()
}

class NavigationState {
    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    suspend fun emit(event: NavigationEvent) {
        _navigationEvents.emit(event)
    }

    var startDestination: String = Screen.PhoneNumber.route
        private set

    fun updateStartDestination(destination: String) {
        if (destination.isNotBlank()) {
            startDestination = destination
        } else {
            Timber.w("Intento de actualizar startDestination con un valor vacío")
        }
    }
}

@Composable
fun rememberNavigationState() = remember { NavigationState() }

class PermissionHandler(
    private val onPermissionGranted: () -> Unit,
    private val onPermissionDenied: () -> Unit
) {
    fun checkPermission(permission: Boolean) {
        if (permission) {
            Timber.d("Permisos concedidos")
            onPermissionGranted()
        } else {
            Timber.d("Permisos denegados")
            onPermissionDenied()
        }
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String = Screen.PhoneNumber.route,
    onRequestPermissions: () -> Unit,
    permissionsGranted: Boolean,
    navigationState: NavigationState = rememberNavigationState(),
    locationTracker: LocationTracker
) {
    val permissionHandler = remember {
        PermissionHandler(
            onPermissionGranted = { Timber.d("Permisos concedidos exitosamente") },
            onPermissionDenied = {
                Timber.w("Permisos denegados, solicitando nuevamente")
                onRequestPermissions()
            }
        )
    }

    LaunchedEffect(permissionsGranted) {
        permissionHandler.checkPermission(permissionsGranted)
    }

    LaunchedEffect(navigationState) {
        navigationState.navigationEvents.collect { event ->
            try {
                when (event) {
                    is NavigationEvent.NavigateToChat -> {
                        val route = Screen.Chat.createRoute(event.chatId)
                        Timber.d("Navegando a chat: $route")
                        navController.navigate(route)
                    }
                    is NavigationEvent.ActivatePanicMode -> {
                        val route = Screen.PanicLocation.createRoute(event.chatId, event.chatType)
                        Timber.d("Activando modo pánico: $route")
                        navController.navigate(route)
                    }
                    is NavigationEvent.NavigateBack -> {
                        Timber.d("Regresando a pantalla anterior")
                        navController.popBackStack()
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error procesando evento de navegación: ${e.message}")
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.PhoneNumber.route) {
            PhoneNumberScreen(navController = navController)
        }
        composable(
            route = Screen.VerificationCode.route,
            arguments = listOf(
                navArgument("phoneNumber") { type = NavType.StringType },
                navArgument("verificationId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            val verificationId = backStackEntry.arguments?.getString("verificationId") ?: ""
            if (phoneNumber.isNotEmpty() && verificationId.isNotEmpty()) {
                VerificationCodeScreen(navController, phoneNumber, verificationId)
            } else {
                Timber.e("Faltan argumentos en VerificationCodeScreen: phoneNumber=$phoneNumber, verificationId=$verificationId")
                navController.popBackStack()
            }
        }
        composable(Screen.ProfileSetup.route) {
            ProfileSetupScreen(navController = navController)
        }
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }
        composable(Screen.Register.route) {
            RegisterScreen(navController = navController)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = {
                    try {
                        Timber.d("Regresando desde SettingsScreen")
                        navController.popBackStack()
                    } catch (e: Exception) {
                        Timber.e(e, "Error al regresar desde SettingsScreen: ${e.message}")
                    }
                },
                onNavigateToRoute = { route ->
                    try {
                        Timber.d("Navegando desde SettingsScreen a: $route")
                        navController.navigate(route)
                    } catch (e: Exception) {
                        Timber.e(e, "Error al navegar desde SettingsScreen a $route: ${e.message}")
                    }
                }
            )
        }
        composable(Screen.Stories.route) { backStackEntry ->
            val currentRoute = backStackEntry.destination.route
            ScaffoldWrapper(navController = navController, currentRoute = currentRoute) {
                StoriesScreen(
                    navController = navController,
                    onNavigateToAddStory = {
                        try {
                            Timber.d("Navegando a AddStory desde StoriesScreen")
                            navController.navigate(Screen.AddStory.route)
                        } catch (e: Exception) {
                            Timber.e(e, "Error al navegar a AddStory: ${e.message}")
                        }
                    }
                )
            }
        }
        composable(Screen.AddStory.route) {
            EmptyScreenPlaceholder(icon = Icons.Default.Add, text = "Agregar Historia")
        }
        composable(Screen.Channels.route) { backStackEntry ->
            val currentRoute = backStackEntry.destination.route
            ScaffoldWrapper(navController = navController, currentRoute = currentRoute) {
                ChannelScreen(
                    channelId = "default",
                    onNavigateUp = {
                        try {
                            Timber.d("Regresando desde ChannelScreen")
                            navController.navigateUp()
                        } catch (e: Exception) {
                            Timber.e(e, "Error al regresar desde ChannelScreen: ${e.message}")
                        }
                    }
                )
            }
        }
        composable(Screen.Communities.route) { backStackEntry ->
            val currentRoute = backStackEntry.destination.route
            ScaffoldWrapper(navController = navController, currentRoute = currentRoute) {
                CommunitiesScreen(
                    onNavigateToCreateCommunity = {
                        try {
                            Timber.d("Navegando a crear comunidad")
                            navController.navigate("community_create")
                        } catch (e: Exception) {
                            Timber.e(e, "Error al navegar a community_create: ${e.message}")
                        }
                    },
                    onNavigateToCommunityDetail = { communityId ->
                        try {
                            Timber.d("Navegando a detalle de comunidad: $communityId")
                            navController.navigate("community_detail/$communityId")
                        } catch (e: Exception) {
                            Timber.e(e, "Error al navegar a community_detail/$communityId: ${e.message}")
                        }
                    }
                )
            }
        }
        composable(Screen.ChatRooms.route) { backStackEntry ->
            val currentRoute = backStackEntry.destination.route
            ScaffoldWrapper(navController = navController, currentRoute = currentRoute) {
                EmptyScreenPlaceholder(icon = Icons.Default.Forum, text = "Salas")
            }
        }
        composable(Screen.Calls.route) { backStackEntry ->
            val currentRoute = backStackEntry.destination.route
            ScaffoldWrapper(navController = navController, currentRoute = currentRoute) {
                CallScreen(
                    onNavigateBack = {
                        try {
                            Timber.d("Regresando desde CallScreen")
                            navController.navigateUp()
                        } catch (e: Exception) {
                            Timber.e(e, "Error al regresar desde CallScreen: ${e.message}")
                        }
                    }
                )
            }
        }
        composable(Screen.Chats.route) { backStackEntry ->
            val currentRoute = backStackEntry.destination.route
            ScaffoldWrapper(navController = navController, currentRoute = currentRoute) {
                ChatScreen(navController = navController, chatId = null, isDetailView = false)
            }
        }
        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId")
            ChatScreen(navController = navController, chatId = chatId, isDetailView = false)
        }
        composable(
            route = Screen.ChatDetail.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId")
            ChatScreen(navController = navController, chatId = chatId, isDetailView = true)
        }
        composable(
            route = Screen.PanicLocation.route,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType },
                navArgument("chatType") { type = NavType.StringType; nullable = false }
            ),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { NavigationConstants.TRANSITION_OFFSET },
                    animationSpec = tween(NavigationConstants.ANIMATION_DURATION)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -NavigationConstants.TRANSITION_OFFSET },
                    animationSpec = tween(NavigationConstants.ANIMATION_DURATION)
                )
            }
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: run {
                Timber.e("Falta chatId en PanicLocationScreen")
                return@composable
            }
            val chatType = backStackEntry.arguments?.getString("chatType")?.let {
                try {
                    ChatType.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    Timber.e(e, "Error parseando chatType: $it")
                    null
                }
            } ?: run {
                Timber.e("Falta chatType válido en PanicLocationScreen")
                return@composable
            }
            RealTimeLocationScreen(
                locationTracker = locationTracker,
                chatId = chatId,
                chatType = chatType,
                onNavigateBack = {
                    try {
                        Timber.d("Regresando desde PanicLocationScreen")
                        navController.popBackStack()
                    } catch (e: Exception) {
                        Timber.e(e, "Error al regresar desde PanicLocationScreen: ${e.message}")
                    }
                }
            )
        }
    }
}
