package com.example.taskapplication.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "message_reactions")
data class MessageReactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val messageId: String,
    val userId: String,
    val reaction: String,
    val createdAt: Long,
    val syncStatus: String = "synced",
    val isDeleted: Boolean = false
) 