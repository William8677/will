package com.williamfq.xhat.ui.screens.chat.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class ChatMenuOption(val title: String, val icon: ImageVector) {

    // Acciones de Chat
    object ViewContact : ChatMenuOption("Ver contacto", Icons.Default.Person)
    object Search : ChatMenuOption("Buscar", Icons.Default.Search)
    object AddToList : ChatMenuOption("Añadir a lista", Icons.Default.AddCircle)
    object ClearChat : ChatMenuOption("Vaciar chat", Icons.Default.Delete)
    object DeleteChat : ChatMenuOption("Eliminar chat", Icons.Default.DeleteForever)
    object ExportChat : ChatMenuOption("Exportar chat", Icons.Default.Download)
    object CreateShortcut : ChatMenuOption("Crear acceso directo", Icons.Default.AddLink)

    // Multimedia y Archivos
    object Files : ChatMenuOption("Archivos", Icons.Default.Folder)
    object Links : ChatMenuOption("Enlaces", Icons.Default.Link)
    object ToggleVoiceRecorder : ChatMenuOption("Grabador de voz", Icons.Default.Mic)
    object ToggleCamera : ChatMenuOption("Cámara", Icons.Default.Videocam)

    // Notificaciones y Privacidad
    object MuteNotifications : ChatMenuOption("Silenciar", Icons.Default.NotificationsOff)
    data class MuteFor(val duration: String) : ChatMenuOption("Silenciar por $duration", Icons.Default.NotificationsOff)
    object TemporaryMessages : ChatMenuOption("Mensajes temporales", Icons.Default.Timer)
    object Report : ChatMenuOption("Reportar", Icons.Default.Report)
    object Block : ChatMenuOption("Bloquear", Icons.Default.Block)

    // Llamadas y Comunicación
    object Call : ChatMenuOption("Llamada", Icons.Default.Call)
    object VideoCall : ChatMenuOption("Videollamada", Icons.Default.VideoCall)
    object ToggleWalkieTalkie : ChatMenuOption("Walkie-Talkie", Icons.Default.Mic)

    // Configuración
    object ChangeWallpaper : ChatMenuOption("Cambiar fondo", Icons.Default.Wallpaper)
    object ToggleChatGPT : ChatMenuOption("ChatGPT", Icons.AutoMirrored.Filled.Chat)
    object ToggleMicrophone : ChatMenuOption("Micrófono", Icons.Default.Mic)
    object ToggleSpeaker : ChatMenuOption("Altavoz", Icons.AutoMirrored.Filled.VolumeUp)
    object ToggleBluetooth : ChatMenuOption("Bluetooth", Icons.Default.Bluetooth)

    companion object {
        fun getAllOptions(): List<ChatMenuOption> = listOf(
            // Acciones de Chat
            ViewContact,
            Search,
            AddToList,
            ClearChat,
            DeleteChat,
            ExportChat,
            CreateShortcut,

            // Multimedia y Archivos
            Files,
            Links,
            ToggleVoiceRecorder,
            ToggleCamera,

            // Notificaciones y Privacidad
            MuteNotifications,
            MuteFor("1 hora"),
            MuteFor("8 horas"),
            MuteFor("1 semana"),
            TemporaryMessages,
            Report,
            Block,

            // Llamadas y Comunicación
            Call,
            VideoCall,
            ToggleWalkieTalkie,

            // Configuración
            ChangeWallpaper,
            ToggleChatGPT,
            ToggleMicrophone,
            ToggleSpeaker,
            ToggleBluetooth
        )
    }
}