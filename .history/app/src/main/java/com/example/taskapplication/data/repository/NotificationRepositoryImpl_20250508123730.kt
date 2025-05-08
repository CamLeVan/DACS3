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
        val notifications = notificationDao.getAllNotifications().map { it.toDomainModel() }
        emit(notifications)
    }

    override fun getNotificationsByUser(userId: String): Flow<List<Notification>> = flow {
        val notifications = notificationDao.getNotificationsByUser(userId.toLong()).map { it.toDomainModel() }
        emit(notifications)
    }

    override suspend fun getNotificationsSync(): List<Notification> = withContext(Dispatchers.IO) {
        notificationDao.getAllNotificationsSync().map { it.toDomainModel() }
    }

    override suspend fun getNotificationById(id: String): Notification? = withContext(Dispatchers.IO) {
        notificationDao.getNotificationById(id.toLong())?.toDomainModel()
    }

    override suspend fun getNotificationByIdSync(id: String): Notification? = withContext(Dispatchers.IO) {
        notificationDao.getNotificationByIdSync(id.toLong())?.toDomainModel()
    }

    override suspend fun getNotificationByServerId(serverId: String): Notification? = withContext(Dispatchers.IO) {
        notificationDao.getNotificationByServerId(serverId)?.toDomainModel()
    }

    override suspend fun getNotificationByServerIdSync(serverId: String): Notification? = withContext(Dispatchers.IO) {
        notificationDao.getNotificationByServerIdSync(serverId)?.toDomainModel()
    }

    override suspend fun createNotification(notification: Notification): Result<Notification> = withContext(Dispatchers.IO) {
        try {
            val entity = notification.toEntity()
            entity.syncStatus = if (networkUtils.isNetworkAvailable()) "synced" else "pending"
            val id = notificationDao.insertNotification(entity)
            val createdNotification = notificationDao.getNotificationById(id)?.toDomainModel()
                ?: return@withContext Result.failure(Exception("Failed to create notification"))
            
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.createNotification(entity.toApiRequest())
                val updatedEntity = response.toEntity(entity)
                notificationDao.updateNotification(updatedEntity)
                Result.success(updatedEntity.toDomainModel())
            } else {
                Result.success(createdNotification)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateNotification(notification: Notification): Result<Notification> = withContext(Dispatchers.IO) {
        try {
            val entity = notification.toEntity()
            entity.syncStatus = if (networkUtils.isNetworkAvailable()) "synced" else "pending"
            notificationDao.updateNotification(entity)
            val updatedNotification = notificationDao.getNotificationById(entity.id)?.toDomainModel()
                ?: return@withContext Result.failure(Exception("Failed to update notification"))
            
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.updateNotification(entity.id, entity.toApiRequest())
                val updatedEntity = response.toEntity(entity)
                notificationDao.updateNotification(updatedEntity)
                Result.success(updatedEntity.toDomainModel())
            } else {
                Result.success(updatedNotification)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteNotification(notification: Notification): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = notification.toEntity()
            if (networkUtils.isNetworkAvailable()) {
                apiService.deleteNotification(entity.id)
                notificationDao.deleteNotification(entity)
                Result.success(Unit)
            } else {
                entity.syncStatus = "pending"
                notificationDao.updateNotification(entity)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncNotifications(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!networkUtils.isNetworkAvailable()) {
                return@withContext Result.failure(Exception("No network connection"))
            }

            val pendingNotifications = notificationDao.getPendingNotificationsSync()
            for (notification in pendingNotifications) {
                when (notification.syncStatus) {
                    "pending" -> {
                        val response = apiService.createNotification(notification.toApiRequest())
                        val updatedEntity = response.toEntity(notification)
                        notificationDao.updateNotification(updatedEntity)
                    }
                    "deleted" -> {
                        apiService.deleteNotification(notification.id)
                        notificationDao.deleteNotification(notification)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncNotificationsByUser(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!networkUtils.isNetworkAvailable()) {
                return@withContext Result.failure(Exception("No network connection"))
            }

            val pendingNotifications = notificationDao.getPendingNotificationsByUserSync(userId.toLong())
            for (notification in pendingNotifications) {
                when (notification.syncStatus) {
                    "pending" -> {
                        val response = apiService.createNotification(notification.toApiRequest())
                        val updatedEntity = response.toEntity(notification)
                        notificationDao.updateNotification(updatedEntity)
                    }
                    "deleted" -> {
                        apiService.deleteNotification(notification.id)
                        notificationDao.deleteNotification(notification)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getNotifications(since: Long?): Result<List<Notification>> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        return try {
            val response = apiService.getNotifications(since)
            if (response.isSuccessful) {
                val notifications = response.body()?.map { it.toDomainModel() } ?: emptyList()
                Result.success(notifications)
            } else {
                Log.e(TAG, "Failed to get notifications: ${response.code()} ${response.message()}")
                Result.failure(Exception("Failed to get notifications: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting notifications", e)
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
