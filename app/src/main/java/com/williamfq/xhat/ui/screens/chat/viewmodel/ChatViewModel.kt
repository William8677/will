package com.williamfq.xhat.ui.screens.chat.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.williamfq.domain.location.LocationTracker
import com.williamfq.domain.model.ChatMessage
import com.williamfq.domain.model.MessageType
import com.williamfq.domain.repository.ChatRepository
import com.williamfq.xhat.service.WalkieTalkieService
import com.williamfq.xhat.ui.screens.chat.model.ChatMenuOption
import com.williamfq.xhat.ui.screens.chat.model.ChatUiState
import com.williamfq.xhat.utils.VoiceRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val walkieTalkieService: WalkieTalkieService,
    private val voiceRecorder: VoiceRecorder,
    private val locationTracker: LocationTracker
) : ViewModel(), ChatViewModelInterface {

    private val _uiState = MutableStateFlow(ChatUiState())
    override val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private var currentChatId: String? = null
    private var currentUserId: String? = null
    private var searchJob: kotlinx.coroutines.Job? = null

    init {
        initializeCollectors()
    }

    private fun initializeCollectors() {
        viewModelScope.launch {
            combine(
                walkieTalkieService.walkieTalkieState,
                chatRepository.observeMessages()
            ) { walkieTalkieState, messages ->
                _uiState.update { state ->
                    state.copy(
                        walkieTalkieState = walkieTalkieState,
                        messages = messages
                    )
                }
            }.collect()
        }
    }

    override fun loadChat(chatId: String) {
        currentChatId = chatId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val chatInfo = chatRepository.getChatInfo(chatId)
                val messages = chatRepository.getChatMessages(chatId)
                currentUserId = chatRepository.getCurrentUserId()
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        chatTitle = chatInfo.title,
                        messages = messages,
                        chatInfo = chatInfo
                    )
                }
            } catch (e: Exception) {
                handleError("Error al cargar el chat", e)
            }
        }
    }

    override fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            try {
                chatRepository.deleteMessage(messageId)
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages.filter { it.messageId != messageId },
                        selectedMessage = null,
                        showMessageOptions = false
                    )
                }
            } catch (e: Exception) {
                handleError("Error al eliminar mensaje", e)
            }
        }
    }

    override fun editMessage(messageId: String, newContent: String) {
        if (newContent.isBlank()) return
        viewModelScope.launch {
            try {
                chatRepository.editMessage(messageId, newContent)
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages.map {
                            if (it.messageId == messageId) it.copy(content = newContent, isEdited = true) else it
                        },
                        selectedMessage = null,
                        showMessageOptions = false,
                        isEditing = false
                    )
                }
            } catch (e: Exception) {
                handleError("Error al editar mensaje", e)
            }
        }
    }

    override fun startEditing(message: ChatMessage) {
        _uiState.update {
            it.copy(selectedMessage = message, isEditing = true, currentMessage = message.content)
        }
    }

    override fun cancelEditing() {
        _uiState.update { it.copy(selectedMessage = null, isEditing = false, currentMessage = "") }
    }

    override fun onMessageChange(message: String) {
        _uiState.update { it.copy(currentMessage = message) }
    }

    override fun onSendMessage() {
        val messageText = uiState.value.currentMessage
        if (messageText.isBlank()) return
        viewModelScope.launch {
            try {
                if (uiState.value.isEditing && uiState.value.selectedMessage != null) {
                    editMessage(uiState.value.selectedMessage!!.messageId, messageText)
                } else {
                    val message = createMessage(messageText) // Ahora es suspendida
                    chatRepository.sendMessage(message)
                    _uiState.update { it.copy(currentMessage = "") }
                }
            } catch (e: Exception) {
                handleError("Error al enviar mensaje", e)
            }
        }
    }

    override fun onAttachmentClick() {
        _uiState.update { it.copy(showAttachmentOptions = true) }
    }

    override fun onMessageClick(message: ChatMessage) {
        _uiState.update { it.copy(selectedMessage = message) }
    }

    override fun onMessageLongPress(message: ChatMessage) {
        viewModelScope.launch {
            val currentUserId = chatRepository.getCurrentUserId()
            _uiState.update {
                it.copy(
                    selectedMessage = message,
                    showMessageOptions = true,
                    canEditMessage = message.senderId == currentUserId && !message.isDeleted
                )
            }
        }
    }

    override fun onMenuClick() {
        _uiState.update { it.copy(showMenu = !it.showMenu) }
    }

    override fun onCallClick() {
        currentChatId?.let { chatId ->
            viewModelScope.launch {
                try {
                    chatRepository.initiateCall(chatId, isVideo = false)
                    _uiState.update { it.copy(isInCall = true) }
                } catch (e: Exception) {
                    handleError("Error al iniciar llamada", e)
                }
            }
        }
    }

    override fun onVideoCallClick() {
        currentChatId?.let { chatId ->
            viewModelScope.launch {
                try {
                    chatRepository.initiateCall(chatId, isVideo = true)
                    _uiState.update { it.copy(isInVideoCall = true) }
                } catch (e: Exception) {
                    handleError("Error al iniciar videollamada", e)
                }
            }
        }
    }

    override fun onWalkieTalkiePressed() {
        currentChatId?.let { chatId ->
            walkieTalkieService.startWalkieTalkie(chatId)
            _uiState.update { it.copy(isWalkieTalkieActive = true) }
        }
    }

    override fun onWalkieTalkieReleased() {
        walkieTalkieService.stopWalkieTalkie()
        _uiState.update { it.copy(isWalkieTalkieActive = false) }
    }

    override fun onMenuOptionSelected(option: ChatMenuOption) {
        viewModelScope.launch {
            handleMenuOption(option)
            _uiState.update { it.copy(showMenu = false) }
        }
    }

    override fun handleMenuOption(option: ChatMenuOption) {
        when (option) {
            ChatMenuOption.ViewContact -> handleViewContact()
            ChatMenuOption.Search -> handleSearch()
            ChatMenuOption.AddToList -> handleAddToList()
            ChatMenuOption.Files -> handleFiles()
            ChatMenuOption.Links -> handleLinks()
            ChatMenuOption.MuteNotifications -> handleMute()
            is ChatMenuOption.MuteFor -> handleMuteFor(option.duration)
            ChatMenuOption.Call -> handleCall()
            ChatMenuOption.VideoCall -> handleVideoCall()
            ChatMenuOption.ChangeWallpaper -> handleChangeWallpaper()
            ChatMenuOption.ClearChat -> handleClearChat()
            ChatMenuOption.DeleteChat -> handleDeleteChat()
            ChatMenuOption.TemporaryMessages -> handleTemporaryMessages()
            ChatMenuOption.Report -> handleReport()
            ChatMenuOption.Block -> handleBlock()
            ChatMenuOption.ExportChat -> handleExportChat()
            ChatMenuOption.CreateShortcut -> handleCreateShortcut()
            ChatMenuOption.ToggleWalkieTalkie -> handleToggleWalkieTalkie()
            ChatMenuOption.ToggleChatGPT -> handleToggleChatGPT()
            ChatMenuOption.ToggleVoiceRecorder -> handleToggleVoiceRecorder()
            ChatMenuOption.ToggleCamera -> handleToggleCamera()
            ChatMenuOption.ToggleMicrophone -> handleToggleMicrophone()
            ChatMenuOption.ToggleSpeaker -> handleToggleSpeaker()
            ChatMenuOption.ToggleBluetooth -> handleToggleBluetooth()
        }
    }

    override fun handleViewContact() {
        _uiState.update { it.copy(showContactInfo = true) }
    }

    override fun handleSearch() {
        _uiState.update { it.copy(isSearchActive = true, searchResults = emptyList()) }
    }

    override fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchMessages()
    }

    private fun searchMessages() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            try {
                val query = _searchQuery.value
                if (query.length >= 3 && currentChatId != null) {
                    _uiState.update { it.copy(isLoading = true) }
                    val results = chatRepository.searchMessages(currentChatId!!, query)
                    _uiState.update { it.copy(searchResults = results, isLoading = false) }
                } else {
                    _uiState.update { it.copy(searchResults = emptyList(), isLoading = false) }
                }
            } catch (e: Exception) {
                handleError("Error al buscar mensajes", e)
            }
        }
    }

    override fun clearSearch() {
        searchJob?.cancel()
        _searchQuery.value = ""
        _uiState.update { it.copy(isSearchActive = false, searchResults = emptyList()) }
    }

    override fun handleAddToList() {
        _uiState.update { it.copy(showAddToListDialog = true) }
    }

    override fun handleFiles() {
        _uiState.update { it.copy(showFilesScreen = true) }
    }

    override fun handleLinks() {
        _uiState.update { it.copy(showLinksScreen = true) }
    }

    override fun handleMute() {
        currentChatId?.let { chatId ->
            viewModelScope.launch {
                chatRepository.muteChat(chatId)
                _uiState.update { it.copy(isMuted = true) }
            }
        }
    }

    override fun handleMuteFor(duration: String) {
        currentChatId?.let { chatId ->
            viewModelScope.launch {
                chatRepository.muteChatFor(chatId, duration)
                _uiState.update { it.copy(isMuted = true, muteDuration = duration) }
            }
        }
    }

    override fun handleCall() {
        onCallClick()
    }

    override fun handleVideoCall() {
        onVideoCallClick()
    }

    override fun handleChangeWallpaper() {
        _uiState.update { it.copy(showWallpaperPicker = true) }
    }

    override fun handleClearChat() {
        currentChatId?.let { chatId ->
            viewModelScope.launch {
                chatRepository.clearChat(chatId)
                _uiState.update { it.copy(messages = emptyList()) }
            }
        }
    }

    override fun handleDeleteChat() {
        currentChatId?.let { chatId ->
            viewModelScope.launch {
                chatRepository.deleteChat(chatId)
                _uiState.update { it.copy(shouldNavigateBack = true) }
            }
        }
    }

    override fun handleTemporaryMessages() {
        _uiState.update { it.copy(showTemporaryMessagesSettings = true) }
    }

    override fun handleReport() {
        _uiState.update { it.copy(showReportDialog = true) }
    }

    override fun handleBlock() {
        currentChatId?.let { chatId ->
            viewModelScope.launch {
                chatRepository.blockChat(chatId)
                _uiState.update { it.copy(isBlocked = true) }
            }
        }
    }

    override fun handleExportChat() {
        currentChatId?.let { chatId ->
            viewModelScope.launch {
                try {
                    val exportUrl = chatRepository.exportChat(chatId)
                    _uiState.update { it.copy(exportUrl = exportUrl) }
                } catch (e: Exception) {
                    handleError("Error al exportar chat", e)
                }
            }
        }
    }

    override fun handleCreateShortcut() {
        currentChatId?.let { chatId ->
            viewModelScope.launch {
                try {
                    chatRepository.createShortcut(chatId)
                    _uiState.update { it.copy(shortcutCreated = true) }
                } catch (e: Exception) {
                    handleError("Error al crear acceso directo", e)
                }
            }
        }
    }

    override fun handleToggleWalkieTalkie() {
        if (uiState.value.isWalkieTalkieActive) onWalkieTalkieReleased() else onWalkieTalkiePressed()
    }

    override fun handleToggleChatGPT() {
        _uiState.update { it.copy(isChatGPTActive = !it.isChatGPTActive) }
    }

    override fun handleToggleVoiceRecorder() {
        _uiState.update { it.copy(isRecording = !it.isRecording) }
    }

    override fun handleToggleCamera() {
        _uiState.update { it.copy(isCameraEnabled = !it.isCameraEnabled) }
    }

    override fun handleToggleMicrophone() {
        _uiState.update { it.copy(isMicrophoneEnabled = !it.isMicrophoneEnabled) }
    }

    override fun handleToggleSpeaker() {
        _uiState.update { it.copy(isSpeakerEnabled = !it.isSpeakerEnabled) }
    }

    override fun handleToggleBluetooth() {
        _uiState.update { it.copy(isBluetoothEnabled = !it.isBluetoothEnabled) }
    }

    override fun startRecording() {
        voiceRecorder.startRecording()
        _uiState.update { it.copy(isRecording = true) }
    }

    override fun stopRecording() {
        voiceRecorder.stopRecording()
        _uiState.update { it.copy(isRecording = false) }
    }

    override fun sendVoiceMessage() {
        viewModelScope.launch {
            try {
                val audioUri = voiceRecorder.getRecordedFileUri() ?: return@launch
                val message = createMessage(audioUri.toString()) // Ahora es suspendida
                chatRepository.sendMessage(message)
            } catch (e: Exception) {
                handleError("Error al enviar mensaje de voz", e)
            }
        }
    }

    override fun toggleRecording() {
        if (uiState.value.isRecording) stopRecording() else startRecording()
    }

    override fun toggleMicrophone() {
        handleToggleMicrophone()
    }

    override fun toggleSpeaker() {
        handleToggleSpeaker()
    }

    override fun toggleCamera() {
        handleToggleCamera()
    }

    override fun toggleBluetooth() {
        handleToggleBluetooth()
    }

    override fun shareLocation() {
        viewModelScope.launch {
            try {
                val location = locationTracker.getCurrentLocation().firstOrNull()
                if (location != null) {
                    val messageContent = "Ubicación: ${location.latitude}, ${location.longitude}"
                    val message = createMessage(messageContent) // Ahora es suspendida
                    chatRepository.sendMessage(message)
                } else {
                    handleError("No se pudo obtener la ubicación", Exception("Ubicación no disponible"))
                }
            } catch (e: Exception) {
                handleError("Error al compartir ubicación", e)
            }
        }
    }

    override fun shareGallery(uri: String) {
        viewModelScope.launch {
            try {
                val message = createMessage("Imagen: $uri") // Ahora es suspendida
                chatRepository.sendMessage(message)
            } catch (e: Exception) {
                handleError("Error al compartir imagen", e)
            }
        }
    }

    override fun shareContact() {
        viewModelScope.launch {
            try {
                val message = createMessage("Contacto: [Nombre del contacto]") // Ahora es suspendida
                chatRepository.sendMessage(message)
            } catch (e: Exception) {
                handleError("Error al compartir contacto", e)
            }
        }
    }

    override fun shareDocument() {
        viewModelScope.launch {
            try {
                val message = createMessage("Documento: [Nombre del documento]") // Ahora es suspendida
                chatRepository.sendMessage(message)
            } catch (e: Exception) {
                handleError("Error al compartir documento", e)
            }
        }
    }

    override fun shareAudio() {
        viewModelScope.launch {
            try {
                val message = createMessage("Audio: [URL o referencia]") // Ahora es suspendida
                chatRepository.sendMessage(message)
            } catch (e: Exception) {
                handleError("Error al compartir audio", e)
            }
        }
    }

    override fun createPoll() {
        viewModelScope.launch {
            try {
                val message = createMessage("Encuesta: ¿Sí o No? A) Sí B) No") // Ahora es suspendida
                chatRepository.sendMessage(message)
            } catch (e: Exception) {
                handleError("Error al crear encuesta", e)
            }
        }
    }

    override fun shareEvent() {
        viewModelScope.launch {
            try {
                val message = createMessage("Evento: Reunión [fecha] [hora]") // Ahora es suspendida
                chatRepository.sendMessage(message)
            } catch (e: Exception) {
                handleError("Error al compartir evento", e)
            }
        }
    }

    override fun shareScreen() {
        viewModelScope.launch {
            try {
                val message = createMessage("Pantalla: [URL o referencia]") // Ahora es suspendida
                chatRepository.sendMessage(message)
            } catch (e: Exception) {
                handleError("Error al compartir pantalla", e)
            }
        }
    }

    override suspend fun createMessage(content: String): ChatMessage {
        return createMessage(content, MessageType.TEXT)
    }

    private suspend fun createMessage(content: String, type: MessageType): ChatMessage {
        return ChatMessage(
            id = 0,
            messageId = UUID.randomUUID().toString(),
            chatId = currentChatId ?: "",
            senderId = currentUserId ?: chatRepository.getCurrentUserId(),
            recipientId = uiState.value.chatInfo?.otherUserId ?: "",
            content = content,
            username = "Tú",
            timestamp = System.currentTimeMillis(),
            type = type
        )
    }

    override fun handleError(message: String, e: Exception) {
        _uiState.update {
            it.copy(error = "$message: ${e.message}", isLoading = false, showMenu = false)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}