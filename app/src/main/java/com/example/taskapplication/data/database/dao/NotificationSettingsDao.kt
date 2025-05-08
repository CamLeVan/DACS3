package com.example.taskapplication.data.database.dao

import androidx.room.*
import com.example.taskapplication.data.database.entities.NotificationSettingsEntity

/**
 * DAO for notification settings
 */
@Dao
interface NotificationSettingsDao {
    /**
     * Get notification settings for a user
     */
    @Query("SELECT * FROM notification_settings WHERE userId = :userId")
    suspend fun getNotificationSettings(userId: String): NotificationSettingsEntity?
    
    /**
     * Insert notification settings
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationSettings(settings: NotificationSettingsEntity)
    
    /**
     * Update notification settings
     */
    @Update
    suspend fun updateNotificationSettings(settings: NotificationSettingsEntity)
    
    /**
     * Delete notification settings
     */
    @Delete
    suspend fun deleteNotificationSettings(settings: NotificationSettingsEntity)
}
