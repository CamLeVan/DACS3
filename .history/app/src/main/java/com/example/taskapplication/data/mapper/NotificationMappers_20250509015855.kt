package com.example.taskapplication.data.mapper

import com.example.taskapplication.data.api.request.NotificationSettingsRequest
import com.example.taskapplication.data.api.request.QuietHoursSettings
import com.example.taskapplication.data.api.response.NotificationResponse
import com.example.taskapplication.data.api.response.NotificationSettingsResponse
import com.example.taskapplication.data.database.entities.NotificationEntity
import com.example.taskapplication.data.database.entities.NotificationSettingsEntity
import com.example.taskapplication.domain.model.Notification
import com.example.taskapplication.domain.model.NotificationSettings
import java.util.*

/**
 * Convert NotificationEntity to Notification domain model
 */
fun NotificationEntity.toDomainModel(): Notification {
    return Notification(
        id = id,
        serverId = serverId ?: "",
        type = type,
        title = title,
        body = body,
        data = data,
        isRead = readAt != null,
        createdAt = createdAt,
        syncStatus = syncStatus,
        lastModified = lastModified
    )
}

/**
 * Convert Notification domain model to NotificationEntity
 */
fun Notification.toEntity(): NotificationEntity {
    return NotificationEntity(
        id = id,
        serverId = serverId,
        type = type,
        title = title,
        body = body,
        data = data,
        readAt = if (isRead) lastModified else null,
        createdAt = createdAt,
        syncStatus = syncStatus,
        lastModified = lastModified
    )
}

/**
 * Convert NotificationResponse to NotificationEntity
 */
fun NotificationResponse.toEntity(existingNotification: NotificationEntity? = null): NotificationEntity {
    return NotificationEntity(
        id = existingNotification?.id ?: UUID.randomUUID().toString(),
        serverId = id,
        type = type,
        title = title,
        body = body,
        data = data ?: emptyMap(),
        readAt = read_at,
        createdAt = created_at,
        syncStatus = "synced",
        lastModified = System.currentTimeMillis()
    )
}

/**
 * Convert NotificationResponse to Notification domain model (legacy method)
 */
fun NotificationResponse.toNotification(): Notification {
    return Notification(
        id = UUID.randomUUID().toString(),
        serverId = id,
        type = type,
        title = title,
        body = body,
        data = data ?: emptyMap(),
        isRead = read_at != null,
        createdAt = created_at,
        syncStatus = "synced",
        lastModified = created_at
    )
}

/**
 * Convert NotificationSettingsEntity to NotificationSettings domain model
 */
fun NotificationSettingsEntity.toDomainModel(): NotificationSettings {
    return NotificationSettings(
        taskAssignments = taskAssignments,
        taskUpdates = taskUpdates,
        taskComments = taskComments,
        teamMessages = teamMessages,
        teamInvitations = teamInvitations,
        quietHoursEnabled = quietHoursEnabled,
        quietHoursStart = quietHoursStart,
        quietHoursEnd = quietHoursEnd
    )
}

/**
 * Convert NotificationSettings domain model to NotificationSettingsEntity
 */
fun NotificationSettings.toEntity(userId: String, syncStatus: String = "pending_update"): NotificationSettingsEntity {
    return NotificationSettingsEntity(
        userId = userId,
        taskAssignments = taskAssignments,
        taskUpdates = taskUpdates,
        taskComments = taskComments,
        teamMessages = teamMessages,
        teamInvitations = teamInvitations,
        quietHoursEnabled = quietHoursEnabled,
        quietHoursStart = quietHoursStart,
        quietHoursEnd = quietHoursEnd,
        syncStatus = syncStatus,
        lastModified = System.currentTimeMillis()
    )
}

/**
 * Convert NotificationSettings domain model to NotificationSettingsRequest
 */
fun NotificationSettings.toApiRequest(): NotificationSettingsRequest {
    return NotificationSettingsRequest(
        task_assignments = taskAssignments,
        task_updates = taskUpdates,
        task_comments = taskComments,
        team_messages = teamMessages,
        team_invitations = teamInvitations,
        quiet_hours = QuietHoursSettings(
            enabled = quietHoursEnabled,
            start = quietHoursStart,
            end = quietHoursEnd
        )
    )
}

/**
 * Convert NotificationSettingsResponse to NotificationSettings domain model
 */
fun NotificationSettingsResponse.toDomainModel(): NotificationSettings {
    return NotificationSettings(
        taskAssignments = task_assignments,
        taskUpdates = task_updates,
        taskComments = task_comments,
        teamMessages = team_messages,
        teamInvitations = team_invitations,
        quietHoursEnabled = quiet_hours.enabled,
        quietHoursStart = quiet_hours.start,
        quietHoursEnd = quiet_hours.end
    )
}
