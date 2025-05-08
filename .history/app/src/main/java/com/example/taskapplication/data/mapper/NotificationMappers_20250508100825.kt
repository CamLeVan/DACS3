package com.example.taskapplication.data.mapper

import com.example.taskapplication.data.api.response.NotificationResponse
import com.example.taskapplication.domain.model.Notification
import java.util.UUID

fun NotificationResponse.toNotification(): Notification {
    return Notification(
        id = UUID.randomUUID().toString(),
        serverId = id.toString(),
        type = type,
        title = title,
        body = body,
        data = data.mapValues { it.value.toString() },
        isRead = isRead,
        createdAt = createdAt,
        syncStatus = "synced",
        lastModified = createdAt
    )
}
