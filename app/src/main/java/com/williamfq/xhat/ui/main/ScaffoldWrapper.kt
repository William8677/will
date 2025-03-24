package com.williamfq.xhat.ui.main

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController

enum class MainScreens(val title: String, val icon: ImageVector, val route: String) {
    CHATS("Chats", Icons.AutoMirrored.Filled.Chat, "chats"),
    STORIES("Historias", Icons.Filled.PlayCircle, "stories"),
    CHANNELS("Canales", Icons.Filled.Campaign, "channels"),
    COMMUNITIES("Comunidades", Icons.Filled.Groups, "communities"),
    CHAT_ROOMS("Salas", Icons.Filled.Forum, "chat_rooms"),
    CALLS("Llamadas", Icons.Filled.Call, "calls");

    companion object {
        fun fromRoute(route: String?): MainScreens =
            entries.find { it.route == route } ?: CHATS
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaffoldWrapper(
    navController: NavHostController,
    currentRoute: String?,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    val currentScreen = MainScreens.fromRoute(currentRoute)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentScreen.title) },
                navigationIcon = {
                    IconButton(onClick = { /* TODO: Abrir menú lateral */ }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menú")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("profile") }) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Perfil")
                    }
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Ajustes")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                MainScreens.entries.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) }
                    )
                }
            }
        },
        modifier = modifier
    ) { padding ->
        content(padding)
    }
}