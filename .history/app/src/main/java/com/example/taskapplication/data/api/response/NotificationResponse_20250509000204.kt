package com.example.taskapplication.data.api.response

/**
 * Response model for notification
 */
data class NotificationResponse(
    val id: String,
    val type: String, // team_invitation, task_assignment, task_update, task_comment, team_message, etc.
    val title: String,
    val body: String,
    val data: Map<String, String>?,
    val read_at: Long?,
    val created_at: Long
)
