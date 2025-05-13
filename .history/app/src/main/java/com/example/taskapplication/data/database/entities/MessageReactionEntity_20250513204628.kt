package com.example.taskapplication.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

// Placeholder: Define actual fields based on your requirements
@Entity(tableName = "message_reactions")
data class MessageReactionEntity(
    @PrimaryKey val id: String,
    val messageId: String,
    val userId: String,
    val reaction: String? = null,
    val serverId: String?,
    val syncStatus: String = "synced", // "synced", "pending"
    val lastModified: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis()
)