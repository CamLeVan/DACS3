package com.example.taskapplication.data.database.dao

import androidx.room.*
import com.example.taskapplication.data.database.entities.AppSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getAppSettings(): Flow<AppSettingsEntity?>
    
    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getAppSettingsSync(): AppSettingsEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppSettings(settings: AppSettingsEntity)
    
    @Update
    suspend fun updateAppSettings(settings: AppSettingsEntity)
    
    @Query("UPDATE app_settings SET current_user_id = :userId WHERE id = 1")
    suspend fun updateCurrentUserId(userId: String?)
    
    @Query("UPDATE app_settings SET theme_mode = :themeMode WHERE id = 1")
    suspend fun updateThemeMode(themeMode: String)
    
    @Query("UPDATE app_settings SET notification_enabled = :enabled WHERE id = 1")
    suspend fun updateNotificationEnabled(enabled: Boolean)
    
    @Query("UPDATE app_settings SET last_sync_timestamp = :timestamp WHERE id = 1")
    suspend fun updateLastSyncTimestamp(timestamp: Long)
    
    @Query("SELECT current_user_id FROM app_settings WHERE id = 1")
    suspend fun getCurrentUserId(): String?
    
    @Query("SELECT current_user_id FROM app_settings WHERE id = 1")
    fun observeCurrentUserId(): Flow<String?>
    
    @Transaction
    suspend fun initializeSettingsIfNeeded() {
        val settings = getAppSettingsSync()
        if (settings == null) {
            insertAppSettings(AppSettingsEntity())
        }
    }
}
