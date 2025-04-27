package com.example.taskapplication.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

// Placeholder: Define actual fields based on your requirements
@Entity(tableName = "team_tasks")
data class TeamTaskEntity(
    @PrimaryKey val id: String, // UUID String
    val teamId: String,
    val title: String,
    val description: String?,
    val assignedUserId: String?,
    val dueDate: Long?,
    val priority: Int,
    val isCompleted: Boolean,
    val serverId: Long?, // Null if not synced
    val syncStatus: String, // "synced", "pending_create", "pending_update", "pending_delete"
    val lastModified: Long,
    val createdAt: Long
) 