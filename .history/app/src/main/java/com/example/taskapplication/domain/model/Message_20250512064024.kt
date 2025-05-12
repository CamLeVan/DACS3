package com.example.taskapplication.domain.model

data class Message(
    val id: String,
    val teamId: String? = null,
    val senderId: String,
    val receiverId: String? = null,
    val content: String,
    val timestamp: Long,
    val fileUrl: String? = null,
    val status: String = "sent",
    val serverId: String? = null,
    val syncStatus: String = "synced",
    val lastModified: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val readBy: List<String> = emptyList(),
    val reactions: List<MessageReaction> = emptyList(),
    val senderName: String? = null,
    val isDeleted: Boolean = false,
    val isRead: Boolean = false,
    val clientTempId: String? = null,
    val attachments: List<Attachment> = emptyList()
)