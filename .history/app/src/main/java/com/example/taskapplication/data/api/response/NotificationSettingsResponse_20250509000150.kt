package com.example.taskapplication.data.api.response

/**
 * Response model for notification settings
 */
data class NotificationSettingsResponse(
    val task_assignments: Boolean,
    val task_updates: Boolean,
    val task_comments: Boolean,
    val team_messages: Boolean,
    val team_invitations: Boolean,
    val quiet_hours: Map<String, String>?
)
