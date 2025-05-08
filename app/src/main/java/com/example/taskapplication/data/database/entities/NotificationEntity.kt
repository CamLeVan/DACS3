package com.example.taskapplication.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.taskapplication.data.database.util.Converters

/**
 * Entity for notification
 */
@Entity(tableName = "notifications")
@TypeConverters(Converters::class)
data class NotificationEntity(
    @PrimaryKey
    val id: String,
    val serverId: String?,
    val type: String, // team_invitation, task_assignment, task_update, task_comment, team_message, etc.
    val title: String,
    val body: String,
    val data: Map<String, String>,
    val readAt: Long?,
    val createdAt: Long,
    val syncStatus: String,
    val lastModified: Long
)
