package com.example.taskapplication.domain.repository

import com.example.taskapplication.domain.model.Notification
import com.example.taskapplication.domain.model.NotificationSettings
import kotlinx.coroutines.flow.Flow
import java.io.IOException

/**
 * Repository interface for managing notifications
 */
interface NotificationRepository {
    /**
     * Get all notifications for the current user
     * @param read Optional filter for read status
     * @return Flow of list of notifications
     */
    fun getNotifications(read: Boolean? = null): Flow<List<Notification>>

    /**
     * Get notification by ID
     * @param id The ID of the notification
     * @return The notification or null if not found
     */
    suspend fun getNotificationById(id: String): Notification?

    /**
     * Mark notification as read
     * @param id The ID of the notification
     * @return Result containing success or an error
     */
    suspend fun markNotificationAsRead(id: String): Result<Unit>

    /**
     * Mark all notifications as read
     * @return Result containing success or an error
     */
    suspend fun markAllNotificationsAsRead(): Result<Unit>

    /**
     * Register device for push notifications
     * @param fcmToken Firebase Cloud Messaging token
     * @return Result containing success or an error
     */
    suspend fun registerDevice(fcmToken: String): Result<Unit>

    /**
     * Unregister device from push notifications
     * @return Result containing success or an error
     */
    suspend fun unregisterDevice(): Result<Unit>

    /**
     * Get notification settings
     * @return Result containing notification settings or an error
     */
    suspend fun getNotificationSettings(): Result<NotificationSettings>

    /**
     * Update notification settings
     * @param settings The new notification settings
     * @return Result containing updated notification settings or an error
     */
    suspend fun updateNotificationSettings(settings: NotificationSettings): Result<NotificationSettings>

    /**
     * Sync notifications with the server
     * @return Result containing success or an error
     */
    suspend fun syncNotifications(): Result<Unit>

    /**
     * Save notification locally
     * @param notification The notification to save
     */
    suspend fun saveNotification(notification: Notification)

    /**
     * Delete notification locally
     * @param id The ID of the notification
     */
    suspend fun deleteNotification(id: String)

    /**
     * Legacy method to get notifications since a specific time
     * @param since The timestamp to get notifications since
     * @return Result containing list of notifications or an error
     */
    suspend fun getNotificationsSince(since: Long? = null): Result<List<Notification>>
}
