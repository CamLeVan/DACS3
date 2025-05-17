package com.example.taskapplication.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personal_tasks")
data class PersonalTaskEntity(
    @PrimaryKey val id: String, // UUID String để hỗ trợ offline-first
    val title: String,
    val description: String?,
    val dueDate: Long?,
    val priority: String, // "low", "medium", "high"
    val status: String, // "pending", "in_progress", "completed", "archived"
    val category: String?, // Danh mục của task
    val tags: List<String>?, // Các tag của task
    val reminderDate: Long?, // Thời gian nhắc nhở
    val serverId: String?, // Null nếu chưa đồng bộ với server
    val syncStatus: String, // "synced", "pending_create", "pending_update", "pending_delete"
    val lastModified: Long,
    val createdAt: Long,
    val updatedAt: Long?
)