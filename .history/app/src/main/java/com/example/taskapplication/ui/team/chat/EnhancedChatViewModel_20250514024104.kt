package com.example.taskapplication.ui.team.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.data.websocket.ChatWebSocketClient
import com.example.taskapplication.domain.model.Message
import com.example.taskapplication.domain.model.MessageReaction
import com.example.taskapplication.domain.repository.MessageRepository
import com.example.taskapplication.domain.repository.TeamRepository
import com.example.taskapplication.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel nâng cao cho màn hình Chat với hỗ trợ WebSocket
 */
@HiltViewModel
class EnhancedChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val teamRepository: TeamRepository,
    private val userRepository: UserRepository,
    private val dataStoreManager: DataStoreManager,
    private val webSocketClient: ChatWebSocketClient,
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
        connectWebSocket()
    }

    /**
     * Connect to WebSocket
     */
    private fun connectWebSocket() {
        viewModelScope.launch {
            try {
                val token = dataStoreManager.authToken.first()
                if (!token.isNullOrEmpty()) {
                    webSocketClient.connect(token)
                    // Sử dụng teamId trực tiếp thay vì chuyển đổi thành Long
                    webSocketClient.subscribeToTeamChat(teamId)
                }
            } catch (e: Exception) {
                // Xử lý lỗi kết nối WebSocket
                _messagesState.value = MessagesState.Error("Không thể kết nối đến chat: ${e.message}")
            }
        }
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
                    _messagesState.value = MessagesState.Error(e.message ?: "Lỗi không xác định")
                }
                .collect { messages ->
                    _messagesState.value = if (messages.isEmpty()) {
                        MessagesState.Empty
                    } else {
                        MessagesState.Success(messages)
                    }

                    // Mark messages as read and check for pending messages
                    messages.forEach { message ->
                        // Mark unread messages from others as read
                        if (!message.isRead && message.senderId != _currentUserId.value) {
                            messageRepository.markMessageAsRead(message.id)
                        }

                        // Check for pending messages that might be stuck
                        if ((message.syncStatus == "pending_create" || message.syncStatus == "pending_update") &&
                            System.currentTimeMillis() - message.lastModified > 30000) { // 30 seconds timeout
                            // Try to resend messages that have been pending for too long
                            if (message.clientTempId != null) {
                                retrySendMessage(message.clientTempId)
                            }
                        }
                    }
                }
        }
    }

    /**
     * Load more messages (pagination)
     */
    fun loadMoreMessages(limit: Int = 20) {
        viewModelScope.launch {
            val currentMessages = when (val state = _messagesState.value) {
                is MessagesState.Success -> state.messages
                else -> return@launch
            }

            if (currentMessages.isEmpty()) return@launch

            val oldestMessage = currentMessages.minByOrNull { it.timestamp }
            oldestMessage?.let {
                val beforeId = it.serverId ?: it.id
                messageRepository.getTeamMessages(teamId, limit, beforeId, null)
                    .catch { e ->
                        // Xử lý lỗi nếu cần
                    }
                    .collect { olderMessages ->
                        if (olderMessages.isNotEmpty()) {
                            val updatedMessages = (currentMessages + olderMessages).distinctBy { it.id }
                            _messagesState.value = MessagesState.Success(updatedMessages)
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

        // Gửi trạng thái đang nhập nếu có nội dung
        if (text.isNotEmpty() && _editingMessage.value == null) {
            sendTypingStatus(true)
        } else if (text.isEmpty()) {
            sendTypingStatus(false)
        }
    }

    /**
     * Send typing status
     */
    private fun sendTypingStatus(isTyping: Boolean) {
        viewModelScope.launch {
            messageRepository.sendTypingStatus(teamId, isTyping)
        }
    }

    /**
     * Add attachment
     */
    fun addAttachment(attachment: com.example.taskapplication.domain.model.Attachment) {
        val currentAttachments = _attachments.value.toMutableList()
        currentAttachments.add(attachment)
        _attachments.value = currentAttachments
    }

    /**
     * Remove attachment
     */
    fun removeAttachment(attachmentId: String) {
        val currentAttachments = _attachments.value.toMutableList()
        currentAttachments.removeIf { it.id == attachmentId }
        _attachments.value = currentAttachments
    }

    /**
     * Process typing status from other users
     */
    fun processTypingStatus(userId: String, isTyping: Boolean, teamId: String) {
        if (teamId != this.teamId || userId == _currentUserId.value) return

        val currentTypingUsers = _typingUsers.value.toMutableMap()

        if (isTyping) {
            // Thêm người dùng vào danh sách đang nhập với timestamp hiện tại
            currentTypingUsers[userId] = System.currentTimeMillis()
        } else {
            // Xóa người dùng khỏi danh sách đang nhập
            currentTypingUsers.remove(userId)
        }

        _typingUsers.value = currentTypingUsers
    }

    /**
     * Clean up expired typing statuses
     * Call this periodically to remove typing statuses that are too old
     */
    fun cleanupTypingStatuses(expirationTimeMs: Long = 5000) {
        val currentTime = System.currentTimeMillis()
        val currentTypingUsers = _typingUsers.value.toMutableMap()

        // Xóa các trạng thái đang nhập quá cũ
        val expiredUsers = currentTypingUsers.filter { (_, timestamp) ->
            currentTime - timestamp > expirationTimeMs
        }.keys

        expiredUsers.forEach { userId ->
            currentTypingUsers.remove(userId)
        }

        if (expiredUsers.isNotEmpty()) {
            _typingUsers.value = currentTypingUsers
        }
    }

    /**
     * Send a message
     */
    fun sendMessage() {
        val text = _messageText.value.trim()
        if (text.isEmpty() && _attachments.value.isEmpty()) return

        viewModelScope.launch {
            _sendMessageState.value = SendMessageState.Sending

            val currentUserId = _currentUserId.value ?: return@launch
            val clientTempId = UUID.randomUUID().toString()
            val currentAttachments = _attachments.value

            // Kiểm tra xem đang chỉnh sửa tin nhắn hay gửi tin nhắn mới
            val editingMsg = _editingMessage.value
            if (editingMsg != null) {
                // Chỉnh sửa tin nhắn
                messageRepository.editMessage(editingMsg.id, text)
                    .onSuccess {
                        _sendMessageState.value = SendMessageState.Success
                        _messageText.value = ""
                        _editingMessage.value = null

                        // Cập nhật UI ngay lập tức bằng cách tải lại tin nhắn
                        loadMessages()
                    }
                    .onFailure { e ->
                        _sendMessageState.value = SendMessageState.Error(e.message ?: "Không thể chỉnh sửa tin nhắn")
                    }
            } else {
                // Tạo tin nhắn mới để hiển thị ngay lập tức trên UI
                val timestamp = System.currentTimeMillis()
                val newMessage = Message(
                    id = UUID.randomUUID().toString(),
                    content = text,
                    senderId = currentUserId,
                    teamId = teamId,
                    receiverId = null,
                    timestamp = timestamp,
                    serverId = null,
                    syncStatus = "pending_create",
                    lastModified = timestamp,
                    createdAt = timestamp,
                    isDeleted = false,
                    isRead = true,
                    clientTempId = clientTempId,
                    attachments = currentAttachments,
                    reactions = emptyList()
                )

                // Cập nhật UI ngay lập tức với tin nhắn mới
                val currentMessages = when (val state = _messagesState.value) {
                    is MessagesState.Success -> state.messages
                    else -> emptyList()
                }
                val updatedMessages = currentMessages + newMessage
                _messagesState.value = MessagesState.Success(updatedMessages)

                // Xóa text và attachments
                _messageText.value = ""
                _attachments.value = emptyList()

                // Gửi trạng thái không còn đang nhập
                sendTypingStatus(false)

                // Gửi tin nhắn lên server
                messageRepository.sendTeamMessage(teamId, text, clientTempId, currentAttachments)
                    .onSuccess {
                        _sendMessageState.value = SendMessageState.Success
                        // Tải lại tin nhắn để cập nhật trạng thái từ server
                        loadMessages()
                    }
                    .onFailure { e ->
                        _sendMessageState.value = SendMessageState.Error(e.message ?: "Không thể gửi tin nhắn")
                    }
            }
        }
    }

    /**
     * Retry sending a failed message
     */
    fun retrySendMessage(clientTempId: String) {
        viewModelScope.launch {
            _sendMessageState.value = SendMessageState.Sending

            try {
                // Find the message with the given clientTempId
                val currentMessages = (_messagesState.value as? MessagesState.Success)?.messages
                val messageToRetry = currentMessages?.find { it.clientTempId == clientTempId }

                if (messageToRetry != null) {
                    // Cập nhật UI ngay lập tức để hiển thị trạng thái "đang gửi"
                    val updatedMessage = messageToRetry.copy(
                        syncStatus = "pending_create",
                        lastModified = System.currentTimeMillis()
                    )

                    // Cập nhật tin nhắn trong cơ sở dữ liệu cục bộ trước
                    messageRepository.updateMessage(updatedMessage)
                }

                // Gọi phương thức retry của repository
                messageRepository.retrySendMessage(clientTempId)
                    .onSuccess {
                        _sendMessageState.value = SendMessageState.Success
                    }
                    .onFailure { e ->
                        _sendMessageState.value = SendMessageState.Error(e.message ?: "Không thể gửi lại tin nhắn")
                    }
            } catch (e: Exception) {
                _sendMessageState.value = SendMessageState.Error("Lỗi khi thử gửi lại tin nhắn: ${e.message}")
            }
        }
    }

    /**
     * Start editing a message
     */
    fun startEditingMessage(message: Message) {
        _editingMessage.value = message
        _messageText.value = message.content
    }

    /**
     * Cancel editing a message
     */
    fun cancelEditingMessage() {
        _editingMessage.value = null
        _messageText.value = ""
    }

    /**
     * Delete a message
     */
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            messageRepository.deleteMessage(messageId)
                .onFailure { e ->
                    // Hiển thị thông báo lỗi nếu cần
                    _sendMessageState.value = SendMessageState.Error(e.message ?: "Không thể xóa tin nhắn")
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
                    // Hiển thị thông báo lỗi nếu cần
                    _sendMessageState.value = SendMessageState.Error(e.message ?: "Không thể thêm biểu cảm vào tin nhắn")
                }
        }
    }

    /**
     * Remove a reaction from a message
     */
    fun removeReaction(messageId: String, reaction: String) {
        viewModelScope.launch {
            messageRepository.removeReaction(messageId, reaction)
                .onFailure { e ->
                    // Hiển thị thông báo lỗi nếu cần
                    _sendMessageState.value = SendMessageState.Error(e.message ?: "Không thể xóa biểu cảm khỏi tin nhắn")
                }
        }
    }

    /**
     * Disconnect WebSocket when ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        webSocketClient.disconnect()
    }
}