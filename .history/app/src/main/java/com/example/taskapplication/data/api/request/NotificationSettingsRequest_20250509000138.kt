package com.example.taskapplication.data.api.request

/**
 * Request model for notification settings
 */
data class NotificationSettingsRequest(
    val task_assignments: Boolean,
    val task_updates: Boolean,
    val task_comments: Boolean,
    val team_messages: Boolean,
    val team_invitations: Boolean,
    val quiet_hours: Map<String, String>
)
