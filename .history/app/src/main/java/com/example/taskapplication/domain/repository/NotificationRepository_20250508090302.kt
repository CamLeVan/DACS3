package com.example.taskapplication.domain.repository

import com.example.taskapplication.domain.model.Notification

interface NotificationRepository {
    suspend fun getNotifications(since: Long? = null): Result<List<Notification>>
    suspend fun registerDevice(deviceId: String, fcmToken: String): Result<Unit>
    suspend fun unregisterDevice(deviceId: String): Result<Unit>
}
