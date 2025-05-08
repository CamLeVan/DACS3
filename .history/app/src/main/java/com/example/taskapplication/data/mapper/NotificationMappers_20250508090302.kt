package com.example.taskapplication.data.mapper

import com.example.taskapplication.data.api.response.NotificationResponse
import com.example.taskapplication.domain.model.Notification

fun NotificationResponse.toNotification(): Notification {
    return Notification(
        id = id.toString(),
        type = type,
        title = title,
        body = body,
        data = data.mapValues { it.value.toString() },
        isRead = isRead,
        createdAt = createdAt
    )
}
