package com.example.taskapplication.data.api.response

data class ChatResponse(
    val data: List<MessageResponse>,
    val meta: ChatMeta
)

data class ChatMeta(
    val has_more: Boolean,
    val next_cursor: String?
)

data class MessageResponse(
    val id: Long,
    val content: String,
    val created_at: String,
    val updated_at: String,
    val user: UserResponse,
    val attachments: List<AttachmentResponse>,
    val reactions: List<ReactionResponse>
)

data class AttachmentResponse(
    val id: Long,
    val filename: String,
    val file_size: Long,
    val mime_type: String,
    val url: String
)

data class ReactionResponse(
    val emoji: String,
    val count: Int,
    val users: List<Long>
) 