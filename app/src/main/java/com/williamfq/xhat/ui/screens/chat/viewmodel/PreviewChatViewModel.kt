package com.williamfq.xhat.ui.screens.chat.viewmodel

import androidx.lifecycle.ViewModel
import com.williamfq.domain.model.ChatMessage
import com.williamfq.domain.model.MessageStatus
import com.williamfq.domain.model.MessageType
import com.williamfq.domain.model.DeletionType
import com.williamfq.xhat.ui.screens.chat.model.ChatMenuOption
import com.williamfq.xhat.ui.screens.chat.model.ChatUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PreviewChatViewModel : ViewModel(), ChatViewModelInterface {
    private val _uiState = MutableStateFlow(
        ChatUiState(
            chatTitle = "Preview Chat",
            messages = listOf(
                ChatMessage(
                    id = 0,
                    messageId = "1",
                    chatId = "preview",
                    content = "Hola",
                    senderId = "user1",
                    recipientId = "user2",  // Corregido de beneficiaryId
                    timestamp = System.currentTimeMillis(),
                    username = "Usuario de Prueba",
                    isRead = false,
                    isSent = true,
                    isDeleted = false,
                    status = MessageStatus.SENT,
                    roomId = "",
                    type = MessageType.TEXT,
                    replyTo = null,
                    mentions = emptyList(),
                    attachments = emptyList(),
                    extraData = emptyMap(),
                    isEdited = false,
                    editedAt = null,
                    deletionType = DeletionType.NONE,
                    autoDestructAt = null,
                    isMediaMessage = false,
                    canBeEdited = true
                )
            ),
            isLoading = false,
            currentMessage = ""
        )
    )

    override val uiState: StateFlow<ChatUiState> = _uiState

    override fun loadChat(chatId: String) {}
    override fun onMessageChange(message: String) {
        _uiState.value = _uiState.value.copy(currentMessage = message)
    }
    override fun onSendMessage() {}
    override fun onAttachmentClick() {}
    override fun onMessageClick(message: ChatMessage) {}
    override fun onMessageLongPress(message: ChatMessage) {}
    override fun onMenuClick() {}
    override fun onCallClick() {}
    override fun onVideoCallClick() {}
    override fun onWalkieTalkiePressed() {}
    override fun onWalkieTalkieReleased() {}
    override fun onMenuOptionSelected(option: ChatMenuOption) {}
    override fun deleteMessage(messageId: String) {}
    override fun editMessage(messageId: String, newContent: String) {}
    override fun startEditing(message: ChatMessage) {}
    override fun cancelEditing() {}
    override fun updateSearchQuery(query: String) {}
    override fun clearSearch() {}
    override fun handleError(message: String, e: Exception) {}
    override fun handleMenuOption(option: ChatMenuOption) {}
    override fun handleViewContact() {}  // Método agregado
    override fun handleSearch() {}
    override fun handleAddToList() {}
    override fun handleFiles() {}
    override fun handleLinks() {}
    override fun handleMute() {}
    override fun handleMuteFor(duration: String) {}
    override fun handleCall() {}
    override fun handleVideoCall() {}
    override fun handleChangeWallpaper() {}
    override fun handleClearChat() {}
    override fun handleDeleteChat() {}
    override fun handleTemporaryMessages() {}
    override fun handleReport() {}
    override fun handleBlock() {}
    override fun handleExportChat() {}
    override fun handleCreateShortcut() {}
    override fun handleToggleWalkieTalkie() {}
    override fun handleToggleChatGPT() {}
    override fun handleToggleVoiceRecorder() {}
    override fun handleToggleCamera() {}
    override fun handleToggleMicrophone() {}
    override fun handleToggleSpeaker() {}
    override fun handleToggleBluetooth() {}
    override fun startRecording() {}
    override fun stopRecording() {}
    override fun sendVoiceMessage() {}
    override fun toggleRecording() {}
    override fun toggleMicrophone() {}
    override fun toggleSpeaker() {}
    override fun toggleCamera() {}
    override fun toggleBluetooth() {}
    override fun shareLocation() {}
    override fun shareGallery(uri: String) {}
    override fun shareContact() {}
    override fun shareDocument() {}
    override fun shareAudio() {}
    override fun createPoll() {}
    override fun shareEvent() {}
    override fun shareScreen() {}

    override suspend fun createMessage(content: String): ChatMessage {
        return ChatMessage(
            id = 0,
            messageId = "preview-message",
            chatId = "preview",
            content = content,
            senderId = "preview-user",
            recipientId = "preview-recipient",
            timestamp = System.currentTimeMillis(),
            username = "Preview User",
            isRead = false,
            isSent = true,
            isDeleted = false,
            status = MessageStatus.SENT,
            roomId = "",
            type = MessageType.TEXT,
            replyTo = null,
            mentions = emptyList(),
            attachments = emptyList(),
            extraData = emptyMap(),
            isEdited = false,
            editedAt = null,
            deletionType = DeletionType.NONE,
            autoDestructAt = null,
            isMediaMessage = false,
            canBeEdited = true
        )
    }
}