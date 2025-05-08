package com.example.taskapplication.data.database.dao

import androidx.room.*
import com.example.taskapplication.data.database.entities.NotificationEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for notifications
 */
@Dao
interface NotificationDao {
    /**
     * Get all notifications
     */
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>
    
    /**
     * Get all unread notifications
     */
    @Query("SELECT * FROM notifications WHERE readAt IS NULL ORDER BY createdAt DESC")
    fun getUnreadNotifications(): Flow<List<NotificationEntity>>
    
    /**
     * Get all read notifications
     */
    @Query("SELECT * FROM notifications WHERE readAt IS NOT NULL ORDER BY createdAt DESC")
    fun getReadNotifications(): Flow<List<NotificationEntity>>
    
    /**
     * Get notification by ID
     */
    @Query("SELECT * FROM notifications WHERE id = :id")
    suspend fun getNotificationById(id: String): NotificationEntity?
    
    /**
     * Insert a new notification
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)
    
    /**
     * Update a notification
     */
    @Update
    suspend fun updateNotification(notification: NotificationEntity)
    
    /**
     * Delete a notification
     */
    @Delete
    suspend fun deleteNotification(notification: NotificationEntity)
    
    /**
     * Mark notification as read
     */
    @Query("UPDATE notifications SET readAt = :timestamp, syncStatus = 'pending_update', lastModified = :timestamp WHERE id = :id")
    suspend fun markNotificationAsRead(id: String, timestamp: Long)
    
    /**
     * Mark all notifications as read
     */
    @Query("UPDATE notifications SET readAt = :timestamp, syncStatus = 'pending_update', lastModified = :timestamp WHERE readAt IS NULL")
    suspend fun markAllNotificationsAsRead(timestamp: Long)
    
    /**
     * Get all pending sync notifications
     */
    @Query("SELECT * FROM notifications WHERE syncStatus IN ('pending_create', 'pending_update', 'pending_delete')")
    suspend fun getPendingSyncNotifications(): List<NotificationEntity>
    
    /**
     * Get notification by server ID
     */
    @Query("SELECT * FROM notifications WHERE serverId = :serverId")
    suspend fun getNotificationByServerId(serverId: String): NotificationEntity?
}
