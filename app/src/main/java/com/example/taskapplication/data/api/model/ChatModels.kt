package com.example.taskapplication.data.api.model

import com.example.taskapplication.data.api.response.UserResponse
import com.example.taskapplication.domain.model.Attachment
import com.example.taskapplication.domain.model.Message
import com.example.taskapplication.domain.model.MessageReaction
import com.example.taskapplication.domain.model.MessageReadStatus
import com.example.taskapplication.domain.model.User
import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Request models
data class SendMessageRequest(
    val message: String? = null,
    val attachments: List<Long>? = null,
    @SerializedName("client_temp_id")
    val clientTempId: String
)

data class EditMessageRequest(
    val message: String
)

data class MarkAsReadRequest(
    @SerializedName("message_id")
    val messageId: Long
)

data class TypingStatusRequest(
    @SerializedName("is_typing")
    val isTyping: Boolean
)

data class ReactionRequest(
    val reaction: String
)

data class SubscribeRequest(
    val event: String,
    val channel: String
)

// Response models
data class ChatHistoryResponse(
    val data: List<ApiMessage>,
    val meta: ChatMeta
)

data class MessageResponse(
    val data: ApiMessage
)

data class MarkAsReadResponse(
    val message: String,
    val data: ApiReadStatus
)

data class TypingStatusResponse(
    val message: String
)

data class DeleteMessageResponse(
    val message: String,
    val data: ApiMessageDelete
)

data class ReactionResponse(
    val message: String,
    val data: ApiReaction
)

data class AttachmentResponse(
    val data: ApiAttachment
)

data class UnreadCountResponse(
    val data: ApiUnreadCount
)

data class SearchMessagesResponse(
    val data: List<ApiSearchResult>,
    val meta: SearchMeta
)

// WebSocket models
data class WebSocketEvent(
    val event: String,
    val data: Any
)

// API data models
data class ApiMessage(
    val id: Long,
    @SerializedName("team_id")
    val teamId: Long,
    @SerializedName("user_id")
    val userId: Long,
    val message: String,
    val attachments: List<ApiAttachment>,
    @SerializedName("client_temp_id")
    val clientTempId: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("deleted_at")
    val deletedAt: String? = null,
    val user: UserResponse,
    @SerializedName("read_status")
    val readStatus: List<ApiReadStatus> = emptyList(),
    val reactions: List<ApiReaction> = emptyList()
)

data class ApiAttachment(
    val id: Long,
    @SerializedName("file_name")
    val fileName: String,
    @SerializedName("file_size")
    val fileSize: Long,
    @SerializedName("file_type")
    val fileType: String,
    val url: String,
    @SerializedName("created_at")
    val createdAt: String? = null
)

data class ApiReadStatus(
    @SerializedName("team_id")
    val teamId: Long? = null,
    @SerializedName("user_id")
    val userId: Long,
    @SerializedName("message_id")
    val messageId: Long,
    @SerializedName("read_at")
    val readAt: String
)

data class ApiReaction(
    @SerializedName("message_id")
    val messageId: Long,
    @SerializedName("user_id")
    val userId: Long,
    val reaction: String,
    @SerializedName("created_at")
    val createdAt: String
)

data class ApiMessageUpdate(
    val id: Long,
    @SerializedName("team_id")
    val teamId: Long,
    val message: String,
    @SerializedName("updated_at")
    val updatedAt: String
)

data class ApiMessageDelete(
    val id: Long,
    @SerializedName("deleted_at")
    val deletedAt: String
)

data class ApiTypingStatus(
    @SerializedName("team_id")
    val teamId: Long,
    @SerializedName("user_id")
    val userId: Long,
    @SerializedName("is_typing")
    val isTyping: Boolean,
    val timestamp: String
)

data class ApiUnreadCount(
    @SerializedName("unread_count")
    val unreadCount: Int,
    @SerializedName("last_read_id")
    val lastReadId: Long
)

data class ApiSearchResult(
    val id: Long,
    @SerializedName("team_id")
    val teamId: Long,
    @SerializedName("user_id")
    val userId: Long,
    val message: String,
    val attachments: List<ApiAttachment>,
    @SerializedName("client_temp_id")
    val clientTempId: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("deleted_at")
    val deletedAt: String? = null,
    val user: UserResponse,
    val highlight: ApiHighlight
)

data class ApiHighlight(
    val message: String
)

data class ChatMeta(
    @SerializedName("oldest_id")
    val oldestId: Long,
    @SerializedName("newest_id")
    val newestId: Long,
    @SerializedName("has_more_older")
    val hasMoreOlder: Boolean,
    @SerializedName("has_more_newer")
    val hasMoreNewer: Boolean
)

data class SearchMeta(
    val total: Int,
    val page: Int,
    @SerializedName("per_page")
    val perPage: Int,
    @SerializedName("has_more")
    val hasMore: Boolean
)
