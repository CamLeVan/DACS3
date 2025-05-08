package com.example.taskapplication.data.api.response

data class ChatResponse(
    val messages: List<MessageResponse>,
    val has_more: Boolean,
    val next_cursor: String?
)

data class MessageResponse(
    val id: Long,
    val team_id: Long?,
    val sender: UserResponse,
    val receiver: UserResponse?,
    val content: String,
    val timestamp: Long,
    val read_by: List<UserResponse>,
    val reactions: List<ReactionResponse>,
    val attachments: List<AttachmentResponse>,
    val created_at: Long,
    val updated_at: Long
)

data class AttachmentResponse(
    val id: Long,
    val filename: String,
    val file_size: Long,
    val mime_type: String,
    val url: String
)

data class ReactionResponse(
    val id: Long,
    val message_id: Long,
    val user: UserResponse,
    val reaction: String,
    val created_at: Long
) 