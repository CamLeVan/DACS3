package com.example.taskapplication.domain.model

data class MessageReaction(
    val id: String,
    val messageId: String,
    val userId: String,
    val reaction: String,
    val serverId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val syncStatus: String = "synced",
    val lastModified: Long = System.currentTimeMillis()
)
