package com.example.taskapplication.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.taskapplication.data.database.util.Converters

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
@TypeConverters(Converters::class)
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
