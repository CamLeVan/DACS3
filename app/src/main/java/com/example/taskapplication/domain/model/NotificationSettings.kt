package com.example.taskapplication.domain.model

/**
 * Domain model for notification settings
 */
data class NotificationSettings(
    val taskAssignments: Boolean = true,
    val taskUpdates: Boolean = true,
    val taskComments: Boolean = true,
    val teamMessages: Boolean = true,
    val teamInvitations: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "07:00"
)
