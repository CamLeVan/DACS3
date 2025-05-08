package com.example.taskapplication.data.api.request

/**
 * Request model for calendar event
 */
data class CalendarEventRequest(
    val title: String,
    val description: String,
    val start_date: String,
    val end_date: String,
    val type: String,
    val team_id: String?,
    val participants: List<String>?
)
