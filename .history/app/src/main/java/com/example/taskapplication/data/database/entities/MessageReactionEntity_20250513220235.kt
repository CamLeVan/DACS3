package com.example.taskapplication.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

// Cấu trúc chính xác theo schema mong đợi
@Entity(tableName = "message_reactions")
data class MessageReactionEntity(
    @PrimaryKey val id: String,
    val messageId: String,
    val userId: String,
    val reaction: String? = null,
    val serverId: String? = null,
    val lastModified: Long = System.currentTimeMillis()
)