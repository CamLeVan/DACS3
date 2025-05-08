package com.example.taskapplication.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for kanban column
 */
@Entity(
    tableName = "kanban_columns",
    foreignKeys = [
        ForeignKey(
            entity = KanbanBoardEntity::class,
            parentColumns = ["id"],
            childColumns = ["boardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("boardId")
    ]
)
data class KanbanColumnEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val boardId: String,
    val order: Int,
    val serverId: String?,
    val syncStatus: String,
    val lastModified: Long
)
