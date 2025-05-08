package com.example.taskapplication.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

// Placeholder: Define actual fields based on your requirements
@Entity(tableName = "message_read_status")
data class MessageReadStatusEntity(
    @PrimaryKey val id: String,
    val messageId: String,
    val userId: String,
    val readAt: Long,
    val serverId: String?,
    val syncStatus: String = "synced", // "synced", "pending"
    val lastModified: Long = System.currentTimeMillis()
)