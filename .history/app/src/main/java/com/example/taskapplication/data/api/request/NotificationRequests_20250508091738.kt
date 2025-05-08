package com.example.taskapplication.data.api.request

data class RegisterDeviceRequest(
    val device_id: String,
    val fcm_token: String,
    val device_type: String,
    val device_name: String
)

data class NotificationSettingsRequest(
    val task_assignments: Boolean,
    val task_updates: Boolean,
    val task_comments: Boolean,
    val team_messages: Boolean,
    val team_invitations: Boolean,
    val quiet_hours: QuietHoursSettings
)

data class QuietHoursSettings(
    val enabled: Boolean,
    val start: String,
    val end: String
) 