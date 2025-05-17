package com.example.taskapplication.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.taskapplication.domain.model.PersonalTask

@Entity(tableName = "personal_tasks")
data class PersonalTaskEntity(
    @PrimaryKey val id: String, // UUID String để hỗ trợ offline-first
    val title: String,
    val description: String?,
    val dueDate: Long?, // deadline
    val priority: String, // "low", "medium", "high"
    val status: String, // "pending", "in_progress", "completed", "overdue"
    val order: Int,
    val userId: String?,
    val serverId: String?, // Null nếu chưa đồng bộ với server
    val syncStatus: String, // Sử dụng PersonalTask.SyncStatus
    val lastModified: Long,
    val createdAt: Long,
    val updatedAt: Long?
)