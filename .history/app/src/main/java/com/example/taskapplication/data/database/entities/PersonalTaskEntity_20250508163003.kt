package com.example.taskapplication.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personal_tasks")
data class PersonalTaskEntity(
    @PrimaryKey val id: String, // UUID String để hỗ trợ offline-first
    val title: String,
    val description: String?,
    val dueDate: Long?,
    val priority: Int,
    val isCompleted: Boolean,
    val serverId: String?, // Null nếu chưa đồng bộ với server
    val syncStatus: String, // "synced", "pending_create", "pending_update", "pending_delete"
    val lastModified: Long,
    val createdAt: Long
)