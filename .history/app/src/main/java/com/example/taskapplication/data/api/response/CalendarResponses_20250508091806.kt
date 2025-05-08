package com.example.taskapplication.data.api.response

data class CalendarEventResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val start_date: String,
    val end_date: String,
    val type: String,
    val team: TeamResponse?,
    val participants: List<UserResponse>,
    val created_at: String,
    val updated_at: String
) 