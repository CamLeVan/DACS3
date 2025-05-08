package com.example.taskapplication.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "message_read_status")
data class MessageReadStatusEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val messageId: String,
    val userId: String,
    val readAt: Long,
    val syncStatus: String = "synced"
) 