package com.example.taskapplication.domain.model

data class MessageReadStatus(
    val id: String,
    val messageId: String,
    val userId: String,
    val readAt: Long,
    val serverId: String? = null,
    val syncStatus: String = "synced",
    val lastModified: Long = System.currentTimeMillis()
)
