package com.example.taskapplication.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for notification settings
 */
@Entity(tableName = "notification_settings")
data class NotificationSettingsEntity(
    @PrimaryKey
    val userId: String,
    val taskAssignments: Boolean,
    val taskUpdates: Boolean,
    val taskComments: Boolean,
    val teamMessages: Boolean,
    val teamInvitations: Boolean,
    val quietHoursEnabled: Boolean,
    val quietHoursStart: String,
    val quietHoursEnd: String,
    val syncStatus: String,
    val lastModified: Long
)
