package com.example.taskapplication.data.api.response

/**
 * Response model for calendar event
 */
data class CalendarEventResponse(
    val id: String,
    val title: String,
    val description: String,
    val start_date: String,
    val end_date: String,
    val type: String,
    val team: TeamInfo?,
    val participants: List<ParticipantResponse>,
    val created_at: String,
    val updated_at: String
)

/**
 * Response model for team info
 */
data class TeamInfo(
    val id: String,
    val name: String
)

/**
 * Response model for participant
 */
data class ParticipantResponse(
    val id: String,
    val name: String,
    val avatar: String?
)
