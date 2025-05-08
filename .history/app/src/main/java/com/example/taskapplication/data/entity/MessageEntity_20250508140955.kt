package com.example.taskapplication.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val content: String,
    val senderId: String,
    val teamId: String?,
    val receiverId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: String = "synced",
    val isDeleted: Boolean = false
) 