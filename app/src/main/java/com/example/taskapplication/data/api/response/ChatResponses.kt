package com.example.taskapplication.data.api.response

data class ChatResponse(
    val messages: List<MessageResponse>,
    val has_more: Boolean,
    val next_cursor: String?
)

data class AttachmentResponse(
    val id: Long,
    val filename: String,
    val file_size: Long,
    val mime_type: String,
    val url: String
)