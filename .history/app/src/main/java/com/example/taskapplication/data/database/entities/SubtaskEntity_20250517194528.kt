package com.example.taskapplication.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.taskapplication.domain.model.PersonalTask

@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = PersonalTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskableId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("taskableId")
    ]
)
data class SubtaskEntity(
    @PrimaryKey val id: String,
    val taskableType: String, // "App\\Models\\PersonalTask" or "App\\Models\\TeamTask"
    val taskableId: String, // ID of the parent task
    val title: String,
    val completed: Boolean,
    val order: Int,
    val serverId: String?, // Null nếu chưa đồng bộ với server
    val syncStatus: String, // Sử dụng PersonalTask.SyncStatus
    val lastModified: Long,
    val createdAt: Long,
    val updatedAt: Long?
)
