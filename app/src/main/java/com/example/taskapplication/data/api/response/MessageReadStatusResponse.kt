package com.example.taskapplication.data.api.response

data class MessageReadStatusResponse(
    val id: Long,
    val messageId: Long,
    val userId: Long,
    val readAt: String,
    val createdAt: String,
    val updatedAt: String
)
