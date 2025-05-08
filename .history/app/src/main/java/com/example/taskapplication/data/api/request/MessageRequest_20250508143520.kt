package com.example.taskapplication.data.api.request

data class MessageRequest(
    val content: String,
    val teamId: String? = null,
    val receiverId: String? = null,
    val senderId: String? = null
) 