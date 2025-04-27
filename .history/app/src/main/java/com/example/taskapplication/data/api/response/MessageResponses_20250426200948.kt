package com.example.taskapplication.data.api.response

data class MessageResponse(
    val id: Long,
    val teamId: Long?,
    val sender: UserResponse,
    val receiver: UserResponse?,
    val content: String,
    val timestamp: Long,
    val readBy: List<UserResponse>,
    val reactions: List<ReactionResponse>
)

data class ReactionResponse(
    val id: Long,
    val messageId: Long,
    val user: UserResponse,
    val reaction: String,
    val timestamp: Long
)

data class UnreadCountResponse(
    val totalUnread: Int,
    val teamUnread: Map<Long, Int>,
    val directUnread: Map<Long, Int>
) 