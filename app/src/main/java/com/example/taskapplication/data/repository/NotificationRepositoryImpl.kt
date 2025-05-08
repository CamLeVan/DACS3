package com.example.taskapplication.data.repository

import android.util.Log
import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.api.request.RegisterDeviceRequest
import com.example.taskapplication.data.database.dao.NotificationDao
import com.example.taskapplication.data.database.dao.NotificationSettingsDao
import com.example.taskapplication.data.mapper.toApiRequest
import com.example.taskapplication.data.mapper.toDomainModel
import com.example.taskapplication.data.mapper.toEntity
import com.example.taskapplication.data.mapper.toNotification
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.domain.model.Notification
import com.example.taskapplication.domain.model.NotificationSettings
import com.example.taskapplication.domain.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao,
    private val notificationSettingsDao: NotificationSettingsDao,
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager,
    private val connectionChecker: ConnectionChecker
) : NotificationRepository {

    private val TAG = "NotificationRepository"

    override fun getNotifications(read: Boolean?): Flow<List<Notification>> {
        return when (read) {
            true -> notificationDao.getReadNotifications()
            false -> notificationDao.getUnreadNotifications()
            null -> notificationDao.getAllNotifications()
        }.map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getNotificationById(id: String): Notification? {
        return notificationDao.getNotificationById(id)?.toDomainModel()
    }

    override suspend fun markNotificationAsRead(id: String): Result<Unit> {
        try {
            val timestamp = System.currentTimeMillis()
            notificationDao.markNotificationAsRead(id, timestamp)
            
            // If online, sync with server
            if (connectionChecker.isNetworkAvailable()) {
                try {
                    // In a real implementation, we would call an API endpoint to mark the notification as read
                    // For now, we'll just assume it's successful
                } catch (e: Exception) {
                    Log.e(TAG, "Error marking notification as read on server", e)
                    // Continue with local update
                }
            }
            
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking notification as read", e)
            return Result.failure(e)
        }
    }

    override suspend fun markAllNotificationsAsRead(): Result<Unit> {
        try {
            val timestamp = System.currentTimeMillis()
            notificationDao.markAllNotificationsAsRead(timestamp)
            
            // If online, sync with server
            if (connectionChecker.isNetworkAvailable()) {
                try {
                    // In a real implementation, we would call an API endpoint to mark all notifications as read
                    // For now, we'll just assume it's successful
                } catch (e: Exception) {
                    Log.e(TAG, "Error marking all notifications as read on server", e)
                    // Continue with local update
                }
            }
            
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking all notifications as read", e)
            return Result.failure(e)
        }
    }

    override suspend fun registerDevice(fcmToken: String): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }
        
        try {
            val deviceId = dataStoreManager.getDeviceId()
            if (deviceId.isEmpty()) {
                return Result.failure(IOException("Device ID not found"))
            }
            
            val request = RegisterDeviceRequest(
                device_id = deviceId,
                fcm_token = fcmToken,
                device_type = "android",
                device_name = android.os.Build.MODEL
            )
            
            val response = apiService.registerDevice(request)
            
            if (response.isSuccessful) {
                return Result.success(Unit)
            } else {
                return Result.failure(IOException("Failed to register device: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registering device", e)
            return Result.failure(e)
        }
    }

    override suspend fun unregisterDevice(): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }
        
        try {
            val deviceId = dataStoreManager.getDeviceId()
            if (deviceId.isEmpty()) {
                return Result.failure(IOException("Device ID not found"))
            }
            
            val response = apiService.unregisterDevice(deviceId)
            
            if (response.isSuccessful) {
                return Result.success(Unit)
            } else {
                return Result.failure(IOException("Failed to unregister device: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering device", e)
            return Result.failure(e)
        }
    }

    override suspend fun getNotificationSettings(): Result<NotificationSettings> {
        try {
            val userId = dataStoreManager.getCurrentUserId() ?: return Result.failure(IOException("User not logged in"))
            
            // Try to get from local database first
            val localSettings = notificationSettingsDao.getNotificationSettings(userId)
            
            if (localSettings != null) {
                return Result.success(localSettings.toDomainModel())
            }
            
            // If not in local database and online, get from server
            if (connectionChecker.isNetworkAvailable()) {
                try {
                    val response = apiService.getNotificationSettings()
                    
                    if (response.isSuccessful && response.body() != null) {
                        val settings = response.body()!!.toDomainModel()
                        
                        // Save to local database
                        notificationSettingsDao.insertNotificationSettings(settings.toEntity(userId, "synced"))
                        
                        return Result.success(settings)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting notification settings from server", e)
                    // Continue with default settings
                }
            }
            
            // If not found anywhere, return default settings
            val defaultSettings = NotificationSettings()
            notificationSettingsDao.insertNotificationSettings(defaultSettings.toEntity(userId, "pending_create"))
            
            return Result.success(defaultSettings)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting notification settings", e)
            return Result.failure(e)
        }
    }

    override suspend fun updateNotificationSettings(settings: NotificationSettings): Result<NotificationSettings> {
        try {
            val userId = dataStoreManager.getCurrentUserId() ?: return Result.failure(IOException("User not logged in"))
            
            // Save to local database
            val settingsEntity = settings.toEntity(userId, "pending_update")
            notificationSettingsDao.updateNotificationSettings(settingsEntity)
            
            // If online, sync with server
            if (connectionChecker.isNetworkAvailable()) {
                try {
                    val request = settings.toApiRequest()
                    val response = apiService.updateNotificationSettings(request)
                    
                    if (response.isSuccessful && response.body() != null) {
                        val updatedSettings = response.body()!!.toDomainModel()
                        
                        // Update local database with synced status
                        notificationSettingsDao.updateNotificationSettings(updatedSettings.toEntity(userId, "synced"))
                        
                        return Result.success(updatedSettings)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating notification settings on server", e)
                    // Continue with local update
                }
            }
            
            return Result.success(settings)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notification settings", e)
            return Result.failure(e)
        }
    }

    override suspend fun syncNotifications(): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }
        
        try {
            // Get last notification check timestamp
            val lastCheckTimestamp = dataStoreManager.getLastNotificationCheckTimestamp()
            
            // Get notifications from server
            val response = apiService.getNotifications(lastCheckTimestamp)
            
            if (response.isSuccessful && response.body() != null) {
                val notifications = response.body()!!
                
                for (notification in notifications) {
                    val existingNotification = notificationDao.getNotificationByServerId(notification.id)
                    
                    if (existingNotification == null) {
                        // New notification
                        val notificationEntity = notification.toEntity()
                        notificationDao.insertNotification(notificationEntity)
                    }
                }
                
                // Update last check timestamp
                dataStoreManager.saveLastNotificationCheckTimestamp(System.currentTimeMillis())
            }
            
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing notifications", e)
            return Result.failure(e)
        }
    }

    override suspend fun saveNotification(notification: Notification) {
        notificationDao.insertNotification(notification.toEntity())
    }

    override suspend fun deleteNotification(id: String) {
        val notification = notificationDao.getNotificationById(id)
        if (notification != null) {
            notificationDao.deleteNotification(notification)
        }
    }

    override suspend fun getNotificationsSince(since: Long?): Result<List<Notification>> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }
        
        try {
            val response = apiService.getNotifications(since)
            
            if (response.isSuccessful && response.body() != null) {
                val notifications = response.body()!!.map { it.toNotification() }
                
                // Save notifications to local database
                for (notification in notifications) {
                    saveNotification(notification)
                }
                
                return Result.success(notifications)
            } else {
                return Result.failure(IOException("Failed to get notifications: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting notifications", e)
            return Result.failure(e)
        }
    }
}
