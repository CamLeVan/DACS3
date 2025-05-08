package com.example.taskapplication.domain.model

/**
 * Domain model for calendar event
 */
data class CalendarEvent(
    val id: String,
    val title: String,
    val description: String,
    val startDate: Long,
    val endDate: Long,
    val type: String, // meeting, deadline, reminder, etc.
    val teamId: String?,
    val teamName: String?,
    val participants: List<EventParticipant>,
    val serverId: String? = null,
    val syncStatus: String = "synced", // synced, pending_create, pending_update, pending_delete
    val lastModified: Long = System.currentTimeMillis()
)

/**
 * Domain model for event participant
 */
data class EventParticipant(
    val id: String,
    val name: String,
    val avatar: String?
)
