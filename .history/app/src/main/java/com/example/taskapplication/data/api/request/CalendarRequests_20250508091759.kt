package com.example.taskapplication.data.api.request

data class CalendarEventRequest(
    val title: String,
    val description: String? = null,
    val start_date: String,
    val end_date: String,
    val type: String,
    val team_id: Long? = null,
    val participants: List<Long>? = null
) 