package com.example.taskapplication.data.api.response

data class ChatResponse(
    val messages: List<MessageResponse>,
    val has_more: Boolean,
    val next_cursor: String?
)

data class AttachmentResponse(
    val id: Long,
    val message_id: Long? = null,
    val file_name: String,
    val file_size: Long,
    val file_type: String,
    val url: String,
    val created_at: Long = System.currentTimeMillis()
)