package com.example.taskapplication.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.taskapplication.data.database.util.Converters

/**
 * Entity for kanban task
 */
@Entity(
    tableName = "kanban_tasks",
    foreignKeys = [
        ForeignKey(
            entity = KanbanColumnEntity::class,
            parentColumns = ["id"],
            childColumns = ["columnId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("columnId"),
        Index("assignedToId")
    ]
)
@TypeConverters(Converters::class)
data class KanbanTaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val columnId: String,
    val priority: String,
    val dueDate: Long?,
    val assignedToId: String?,
    val assignedToName: String?,
    val assignedToAvatar: String?,
    val position: Int,
    val serverId: String?,
    val syncStatus: String,
    val lastModified: Long
)
