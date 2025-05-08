package com.example.taskapplication.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

// Placeholder: Define actual fields based on your requirements
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String, // UUID String
    val teamId: String? = null, // Null if personal message
    val senderId: String,
    val receiverId: String? = null, // Null if team message
    val content: String,
    val timestamp: Long,
    val serverId: String?, // Null if not synced
    val syncStatus: String, // "synced", "pending"
    val lastModified: Long,
    val createdAt: Long,
    val isDeleted: Boolean = false,
    val isRead: Boolean = false
)