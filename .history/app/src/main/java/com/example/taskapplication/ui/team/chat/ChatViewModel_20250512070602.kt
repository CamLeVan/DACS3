package com.example.taskapplication.ui.team.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.domain.model.Message
import com.example.taskapplication.domain.model.MessageReaction
import com.example.taskapplication.domain.repository.MessageRepository
import com.example.taskapplication.domain.repository.TeamRepository
import com.example.taskapplication.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for the Chat screen
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val teamRepository: TeamRepository,
    private val userRepository: UserRepository,
    private val dataStoreManager: DataStoreManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Team ID from navigation arguments
    private val teamId: String = checkNotNull(savedStateHandle.get<String>("teamId"))

    // Current user ID
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId

    // State for messages
    private val _messagesState = MutableStateFlow<MessagesState>(MessagesState.Loading)
    val messagesState: StateFlow<MessagesState> = _messagesState

    // State for sending messages
    private val _sendMessageState = MutableStateFlow<SendMessageState>(SendMessageState.Idle)
    val sendMessageState: StateFlow<SendMessageState> = _sendMessageState

    // State for team name
    private val _teamName = MutableStateFlow<String?>(null)
    val teamName: StateFlow<String?> = _teamName

    // Message input text
    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText

    // Typing status
    private val _typingUsers = MutableStateFlow<Map<String, Long>>(emptyMap())
    val typingUsers = _typingUsers.asStateFlow()

    // Attachments
    private val _attachments = MutableStateFlow<List<com.example.taskapplication.domain.model.Attachment>>(emptyList())
    val attachments = _attachments.asStateFlow()

    // Message being edited
    private val _editingMessage = MutableStateFlow<Message?>(null)
    val editingMessage = _editingMessage.asStateFlow()

    // Initialize
    init {
        loadCurrentUserId()
        loadMessages()
        loadTeamDetails()
    }

    /**
     * Load current user ID
     */
    private fun loadCurrentUserId() {
        viewModelScope.launch {
            _currentUserId.value = dataStoreManager.getCurrentUserId()
        }
    }

    /**
     * Load messages for the team
     */
    fun loadMessages() {
        viewModelScope.launch {
            _messagesState.value = MessagesState.Loading

            messageRepository.getTeamMessages(teamId)
                .catch { e ->
                    _messagesState.value = MessagesState.Error(e.message ?: "Unknown error")
                }
                .collect { messages ->
                    _messagesState.value = if (messages.isEmpty()) {
                        MessagesState.Empty
                    } else {
                        MessagesState.Success(messages)
                    }

                    // Mark messages as read
                    messages.forEach { message ->
                        if (!message.isRead && message.senderId != _currentUserId.value) {
                            messageRepository.markMessageAsRead(message.id)
                        }
                    }
                }
        }
    }

    /**
     * Load team details
     */
    private fun loadTeamDetails() {
        viewModelScope.launch {
            teamRepository.getTeamById(teamId)
                .collect { team ->
                    _teamName.value = team?.name
                }
        }
    }

    /**
     * Update message text
     */
    fun updateMessageText(text: String) {
        _messageText.value = text
    }

    /**
     * Send a message
     */
    fun sendMessage() {
        val text = _messageText.value.trim()
        if (text.isEmpty()) return

        viewModelScope.launch {
            _sendMessageState.value = SendMessageState.Sending

            val currentUserId = _currentUserId.value ?: return@launch

            messageRepository.sendTeamMessage(teamId, text)
                .onSuccess {
                    _sendMessageState.value = SendMessageState.Success
                    _messageText.value = ""
                }
                .onFailure { e ->
                    _sendMessageState.value = SendMessageState.Error(e.message ?: "Failed to send message")
                }
        }
    }

    /**
     * Delete a message
     */
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            messageRepository.deleteMessage(messageId)
                .onFailure { e ->
                    // Handle error if needed
                }
        }
    }

    /**
     * Add a reaction to a message
     */
    fun addReaction(messageId: String, reaction: String) {
        viewModelScope.launch {
            messageRepository.addReaction(messageId, reaction)
                .onFailure { e ->
                    // Handle error if needed
                }
        }
    }

    /**
     * Remove a reaction from a message
     */
    fun removeReaction(messageId: String, reactionId: String) {
        viewModelScope.launch {
            messageRepository.removeReaction(messageId, reactionId)
                .onFailure { e ->
                    // Handle error if needed
                }
        }
    }
}

/**
 * State for messages
 */
sealed class MessagesState {
    object Loading : MessagesState()
    object Empty : MessagesState()
    data class Success(val messages: List<Message>) : MessagesState()
    data class Error(val message: String) : MessagesState()
}

/**
 * State for sending messages
 */
sealed class SendMessageState {
    object Idle : SendMessageState()
    object Sending : SendMessageState()
    object Success : SendMessageState()
    data class Error(val message: String) : SendMessageState()
}
