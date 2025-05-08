package com.example.taskapplication.data.api.response

data class MessageResponse(
    val id: Long,
    val content: String,
    val teamId: Long? = null,
    val senderId: Long,
    val receiverId: Long? = null,
    val timestamp: Long,
    val lastModified: Long,
    val createdAt: Long,
    val readBy: List<Long> = emptyList(),
    val reactions: List<ReactionResponse> = emptyList()
) 