package com.example.taskapplication.data.api.response

data class ReactionResponse(
    val id: Long,
    val messageId: Long,
    val userId: Long,
    val reaction: String,
    val createdAt: Long
) 