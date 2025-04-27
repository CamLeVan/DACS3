package com.example.taskapplication.domain.model

data class Message(
    val id: String,
    val teamId: String? = null,
    val senderId: String,
    val receiverId: String? = null,
    val content: String,
    val timestamp: Long,
    val serverId: Long? = null,
    val syncStatus: String = "synced",
    val lastModified: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val readBy: List<String> = emptyList(),
    val reactions: List<MessageReaction> = emptyList()
)

data class MessageReaction(
    val id: String,
    val messageId: String,
    val userId: String,
    val reaction: String,
    val serverId: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class MessageReadStatus(
    val id: String,
    val messageId: String,
    val userId: String,
    val readAt: Long,
    val serverId: Long? = null
) 