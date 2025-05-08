package com.example.taskapplication.data.repository

import android.util.Log
import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.api.request.RegisterDeviceRequest
import com.example.taskapplication.data.database.dao.NotificationDao
import com.example.taskapplication.data.database.entities.NotificationEntity
import com.example.taskapplication.data.mapper.toApiRequest
import com.example.taskapplication.data.mapper.toDomainModel
import com.example.taskapplication.data.mapper.toEntity
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.domain.model.Notification
import com.example.taskapplication.domain.repository.NotificationRepository
import com.example.taskapplication.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val notificationDao: NotificationDao,
    private val connectionChecker: ConnectionChecker,
    private val dataStoreManager: DataStoreManager,
    private val networkUtils: NetworkUtils
) : NotificationRepository {

    private val TAG = "NotificationRepository"
    
    override fun getNotifications(): Flow<List<Notification>> = flow {
        notificationDao.getAllNotifications().collect { entities ->
            emit(entities.map { it.toDomainModel() })
        }
    }

    override fun getNotificationsSync(): Flow<List<Notification>> = flow {
        notificationDao.getAllNotificationsSync().collect { entities ->
            emit(entities.map { it.toDomainModel() })
        }
    }

    override suspend fun getNotificationById(id: String): Notification? {
        return notificationDao.getNotificationById(id)?.toDomainModel()
    }

    override suspend fun getNotificationByIdSync(id: String): Notification? {
        return notificationDao.getNotificationByIdSync(id)?.toDomainModel()
    }

    override suspend fun getNotificationByServerId(serverId: String): Notification? {
        return notificationDao.getNotificationByServerId(serverId)?.toDomainModel()
    }

    override suspend fun getNotificationByServerIdSync(serverId: String): Notification? {
        return notificationDao.getNotificationByServerIdSync(serverId)?.toDomainModel()
    }

    override suspend fun createNotification(notification: Notification): Result<Notification> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                val entity = notification.toEntity()
                entity.syncStatus = "pending"
                notificationDao.insertNotification(entity)
                Result.success(entity.toDomainModel())
            } else {
                val response = apiService.createNotification(notification.toApiRequest())
                val entity = response.toEntity()
                entity.syncStatus = "synced"
                notificationDao.insertNotification(entity)
                Result.success(entity.toDomainModel())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateNotification(notification: Notification): Result<Notification> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                val entity = notification.toEntity()
                entity.syncStatus = "pending"
                notificationDao.updateNotification(entity)
                Result.success(entity.toDomainModel())
            } else {
                val response = apiService.updateNotification(notification.id, notification.toApiRequest())
                val entity = response.toEntity()
                entity.syncStatus = "synced"
                notificationDao.updateNotification(entity)
                Result.success(entity.toDomainModel())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteNotification(notification: Notification): Result<Unit> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                val entity = notification.toEntity()
                entity.syncStatus = "pending"
                notificationDao.updateNotification(entity)
                Result.success(Unit)
            } else {
                apiService.deleteNotification(notification.id)
                notificationDao.deleteNotification(notification.toEntity())
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncNotifications(): Result<Unit> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                return Result.failure(Exception("No network connection"))
            }

            val pendingNotifications = notificationDao.getPendingNotificationsSync()
            for (notification in pendingNotifications) {
                when (notification.syncStatus) {
                    "pending" -> {
                        val response = apiService.createNotification(notification.toApiRequest())
                        val entity = response.toEntity()
                        entity.syncStatus = "synced"
                        notificationDao.updateNotification(entity)
                    }
                }
            }

            val serverNotifications = apiService.getNotifications()
            for (notification in serverNotifications) {
                val entity = notification.toEntity()
                entity.syncStatus = "synced"
                notificationDao.insertNotification(entity)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncNotificationsByUser(userId: String): Result<Unit> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                return Result.failure(Exception("No network connection"))
            }

            val pendingNotifications = notificationDao.getPendingNotificationsByUserSync(userId)
            for (notification in pendingNotifications) {
                when (notification.syncStatus) {
                    "pending" -> {
                        val response = apiService.createNotification(notification.toApiRequest())
                        val entity = response.toEntity()
                        entity.syncStatus = "synced"
                        notificationDao.updateNotification(entity)
                    }
                }
            }

            val serverNotifications = apiService.getNotificationsByUser(userId)
            for (notification in serverNotifications) {
                val entity = notification.toEntity()
                entity.syncStatus = "synced"
                notificationDao.insertNotification(entity)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun registerDevice(deviceId: String, fcmToken: String): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        return try {
            val request = RegisterDeviceRequest(
                device_id = deviceId,
                fcm_token = fcmToken,
                device_type = "android",
                device_name = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL,
                user_id = currentUserId
            )
            
            val response = apiService.registerDevice(request)
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Log.e(TAG, "Failed to register device: ${response.code()} ${response.message()}")
                Result.failure(Exception("Failed to register device: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registering device", e)
            Result.failure(e)
        }
    }
    
    override suspend fun unregisterDevice(deviceId: String): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        return try {
            val response = apiService.unregisterDevice(deviceId)
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Log.e(TAG, "Failed to unregister device: ${response.code()} ${response.message()}")
                Result.failure(Exception("Failed to unregister device: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering device", e)
            Result.failure(e)
        }
    }
}
