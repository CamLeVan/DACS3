package com.example.taskapplication.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity đại diện cho tệp đính kèm trong tin nhắn
 */
@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("messageId")]
)
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val messageId: String? = null,
    val fileName: String,
    val fileSize: Long,
    val fileType: String,
    val url: String,
    val serverId: String? = null,
    val syncStatus: String, // "synced", "pending"
    val createdAt: Long
)
