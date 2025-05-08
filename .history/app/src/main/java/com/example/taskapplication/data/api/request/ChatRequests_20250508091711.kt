package com.example.taskapplication.data.api.request

data class MessageRequest(
    val content: String
)

data class TypingStatusRequest(
    val is_typing: Boolean
) 