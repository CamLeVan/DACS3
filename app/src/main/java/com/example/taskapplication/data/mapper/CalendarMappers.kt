package com.example.taskapplication.data.mapper

import com.example.taskapplication.data.api.request.CalendarEventRequest
import com.example.taskapplication.data.api.response.CalendarEventResponse
import com.example.taskapplication.data.api.response.ParticipantResponse
import com.example.taskapplication.data.database.entities.CalendarEventEntity
import com.example.taskapplication.domain.model.CalendarEvent
import com.example.taskapplication.domain.model.EventParticipant
import java.text.SimpleDateFormat
import java.util.*

private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

/**
 * Convert CalendarEventEntity to CalendarEvent domain model
 */
fun CalendarEventEntity.toDomainModel(): CalendarEvent {
    return CalendarEvent(
        id = id,
        title = title,
        description = description,
        startDate = startDate,
        endDate = endDate,
        type = type,
        teamId = teamId,
        teamName = teamName,
        participants = participants,
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = lastModified
    )
}

/**
 * Convert CalendarEvent domain model to CalendarEventEntity
 */
fun CalendarEvent.toEntity(): CalendarEventEntity {
    return CalendarEventEntity(
        id = id,
        title = title,
        description = description,
        startDate = startDate,
        endDate = endDate,
        type = type,
        teamId = teamId,
        teamName = teamName,
        participants = participants,
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = lastModified
    )
}

/**
 * Convert CalendarEventResponse to CalendarEventEntity
 */
fun CalendarEventResponse.toEntity(existingEvent: CalendarEventEntity? = null): CalendarEventEntity {
    return CalendarEventEntity(
        id = existingEvent?.id ?: UUID.randomUUID().toString(),
        title = title,
        description = description,
        startDate = parseDate(start_date),
        endDate = parseDate(end_date),
        type = type,
        teamId = team?.id,
        teamName = team?.name,
        participants = participants.map { it.toDomainModel() },
        serverId = id,
        syncStatus = "synced",
        lastModified = System.currentTimeMillis()
    )
}

/**
 * Convert ParticipantResponse to EventParticipant domain model
 */
fun ParticipantResponse.toDomainModel(): EventParticipant {
    return EventParticipant(
        id = id,
        name = name,
        avatar = avatar
    )
}

/**
 * Convert CalendarEvent domain model to CalendarEventRequest
 */
fun CalendarEvent.toApiRequest(): CalendarEventRequest {
    return CalendarEventRequest(
        title = title,
        description = description,
        start_date = formatDate(startDate),
        end_date = formatDate(endDate),
        type = type,
        team_id = teamId,
        participants = participants.map { it.id }
    )
}

/**
 * Parse date string to timestamp
 */
private fun parseDate(dateString: String): Long {
    return try {
        dateFormat.parse(dateString)?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}

/**
 * Format timestamp to date string
 */
private fun formatDate(timestamp: Long): String {
    return dateFormat.format(Date(timestamp))
}
