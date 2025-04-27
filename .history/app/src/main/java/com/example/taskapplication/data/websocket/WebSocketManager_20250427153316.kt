package com.example.taskapplication.data.websocket

import android.util.Log
// import com.example.taskapplication.data.repository.MessageRepository
import com.example.taskapplication.domain.model.Message
import com.example.taskapplication.domain.model.MessageReadStatus
import com.example.taskapplication.domain.model.MessageReaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketManager @Inject constructor(
    private val messageRepository: com.example.taskapplication.domain.repository.MessageRepository,
    private val scope: CoroutineScope
) {
    private var webSocket: WebSocket? = null
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<ChatEvent>()
    val events = _events.asSharedFlow()

    private val TAG = "WebSocketManager"

    fun connect(serverUrl: String, authToken: String, teamId: String) {
        val teamIdLong = teamId.toLongOrNull() ?: return
        if (_connectionState.value == ConnectionState.CONNECTED ||
            _connectionState.value == ConnectionState.CONNECTING) {
            return
        }

        val request = Request.Builder()
            .url("$serverUrl/reverb?token=$authToken")
            .build()

        webSocket = OkHttpClient().newWebSocket(request, createWebSocketListener(teamIdLong))
        _connectionState.value = ConnectionState.CONNECTING
    }

    private fun createWebSocketListener(teamId: Long): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = ConnectionState.CONNECTED

                // Subscribe to channel
                webSocket.send(
                    JSONObject().apply {
                        put("event", "subscribe")
                        put("channel", "private-teams.$teamId")
                    }.toString()
                )

                Log.d(TAG, "WebSocket connected and subscribed to team $teamId")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "WebSocket message received: $text")
                scope.launch {
                    processMessage(text)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = ConnectionState.DISCONNECTED
                Log.d(TAG, "WebSocket closed: $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = ConnectionState.ERROR
                Log.e(TAG, "WebSocket failure", t)
                // Implement retry logic
            }
        }
    }

    private suspend fun processMessage(text: String) {
        try {
            val json = JSONObject(text)
            val eventName = json.optString("event")
            val data = json.optJSONObject("data")

            when (eventName) {
                "new-chat-message" -> {
                    val message = parseMessage(data)
                    messageRepository.saveMessage(message)
                    _events.emit(ChatEvent.NewMessage(message))
                }
                "message-read" -> {
                    val readStatus = parseReadStatus(data)
                    messageRepository.saveReadStatus(readStatus)
                    _events.emit(ChatEvent.MessageRead(readStatus))
                }
                "user-typing" -> {
                    val userId = data?.optLong("user_id") ?: return
                    val isTyping = data.optBoolean("is_typing")
                    _events.emit(ChatEvent.UserTyping(userId.toString(), isTyping))
                }
                "message-reaction-updated" -> {
                    val messageId = data?.optLong("message_id") ?: return
                    val userId = data.optLong("user_id")
                    val reaction = data.optString("reaction")
                    val action = data.optString("action")
                    _events.emit(ChatEvent.MessageReaction(messageId.toString(), userId.toString(), reaction, action))
                }
                "message-updated" -> {
                    val message = parseMessage(data)
                    messageRepository.updateMessage(message)
                    _events.emit(ChatEvent.MessageUpdated(message))
                }
                "message-deleted" -> {
                    val messageId = data?.optLong("message_id") ?: return
                    messageRepository.markMessageAsDeleted(messageId)
                    _events.emit(ChatEvent.MessageDeleted(messageId.toString()))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing message", e)
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
        Log.d(TAG, "WebSocket disconnected")
    }

    fun sendTypingStatus(teamId: String, isTyping: Boolean) {
        val teamIdLong = teamId.toLongOrNull() ?: return
        val json = JSONObject().apply {
            put("event", "typing")
            put("channel", "private-teams.$teamIdLong")
            put("data", JSONObject().apply {
                put("is_typing", isTyping)
            })
        }
        webSocket?.send(json.toString())
    }

    private fun parseMessage(data: JSONObject?): Message {
        if (data == null) {
            throw IllegalArgumentException("Message data is null")
        }

        return Message(
            id = data.optLong("id").toString(),
            teamId = data.optLong("team_id").toString(),
            senderId = data.optLong("sender_id").toString(),
            senderName = data.optString("sender_name"),
            content = data.optString("content"),
            timestamp = data.optLong("timestamp"),
            fileUrl = data.optString("file_url").takeIf { it.isNotEmpty() },
            status = "synced",
            serverId = data.optLong("id").toString(),
            syncStatus = "synced",
            lastModified = System.currentTimeMillis(),
            createdAt = data.optLong("timestamp")
        )
    }

    private fun parseReadStatus(data: JSONObject?): MessageReadStatus {
        if (data == null) {
            throw IllegalArgumentException("Read status data is null")
        }

        return MessageReadStatus(
            id = System.currentTimeMillis().toString(),
            messageId = data.optLong("message_id").toString(),
            userId = data.optLong("user_id").toString(),
            readAt = data.optLong("read_at"),
            serverId = data.optLong("id").toString(),
            syncStatus = "synced",
            lastModified = System.currentTimeMillis()
        )
    }
}

sealed class ChatEvent {
    data class NewMessage(val message: Message) : ChatEvent()
    data class MessageRead(val readStatus: MessageReadStatus) : ChatEvent()
    data class UserTyping(val userId: String, val isTyping: Boolean) : ChatEvent()
    data class MessageReaction(val messageId: String, val userId: String, val reaction: String, val action: String) : ChatEvent()
    data class MessageUpdated(val message: Message) : ChatEvent()
    data class MessageDeleted(val messageId: String) : ChatEvent()
}

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, ERROR
}