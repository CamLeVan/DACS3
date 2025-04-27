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
    val serverId: Long?
    // Add other fields as needed
) 