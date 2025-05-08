package com.example.taskapplication.data.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.taskapplication.data.database.util.Converters
import com.example.taskapplication.domain.model.EventParticipant

/**
 * Entity for calendar event
 */
@Entity(
    tableName = "calendar_events",
    indices = [
        Index("teamId")
    ]
)
@TypeConverters(Converters::class)
data class CalendarEventEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val startDate: Long,
    val endDate: Long,
    val type: String, // meeting, deadline, reminder, etc.
    val teamId: String?,
    val teamName: String?,
    val participants: List<EventParticipant>,
    val serverId: String?,
    val syncStatus: String,
    val lastModified: Long
)
