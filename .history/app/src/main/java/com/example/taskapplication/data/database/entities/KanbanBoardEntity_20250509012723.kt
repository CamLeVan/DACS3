package com.example.taskapplication.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.taskapplication.data.database.util.Converters

/**
 * Entity for kanban board
 */
@Entity(
    tableName = "kanban_boards",
    indices = [
        Index("teamId")
    ]
)
@TypeConverters(Converters::class)
data class KanbanBoardEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val teamId: String,
    val serverId: String?,
    val syncStatus: String,
    val lastModified: Long
)
