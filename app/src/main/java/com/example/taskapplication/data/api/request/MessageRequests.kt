package com.example.taskapplication.data.api.request

data class MessageRequest(
    val content: String
)

data class DirectMessageRequest(
    val receiverId: Long,
    val content: String
)

data class ReactionRequest(
    val reaction: String
) 