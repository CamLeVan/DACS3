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
    
    override fun getNotifications(userId: String): Flow<List<Notification>> = flow {
        emit(notificationDao.getNotifications(userId).map { it.toDomainModel() })
    }.flowOn(Dispatchers.IO)

    override fun getNotificationsSync(userId: String): List<Notification> {
        return notificationDao.getNotificationsSync(userId).map { it.toDomainModel() }
    }

    override fun getNotificationById(id: String): Flow<Notification?> = flow {
        emit(notificationDao.getNotificationById(id)?.toDomainModel())
    }.flowOn(Dispatchers.IO)

    override fun getNotificationByIdSync(id: String): Notification? {
        return notificationDao.getNotificationByIdSync(id)?.toDomainModel()
    }

    override fun getNotificationByServerId(serverId: String): Flow<Notification?> = flow {
        emit(notificationDao.getNotificationByServerId(serverId)?.toDomainModel())
    }.flowOn(Dispatchers.IO)

    override fun getNotificationByServerIdSync(serverId: String): Notification? {
        return notificationDao.getNotificationByServerIdSync(serverId)?.toDomainModel()
    }

    override suspend fun createNotification(notification: Notification) {
        withContext(Dispatchers.IO) {
            val entity = notification.toEntity()
            notificationDao.insertNotification(entity)

            if (networkUtils.isNetworkAvailable()) {
                try {
                    val response = apiService.createNotification(entity.toApiRequest())
                    val updatedEntity = entity.copy(
                        serverId = response.id.toString(),
                        syncStatus = "synced"
                    )
                    notificationDao.updateNotification(updatedEntity)
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }

    override suspend fun updateNotification(notification: Notification) {
        withContext(Dispatchers.IO) {
            val entity = notification.toEntity()
            notificationDao.updateNotification(entity)

            if (networkUtils.isNetworkAvailable()) {
                try {
                    apiService.updateNotification(entity.serverId!!, entity.toApiRequest())
                    val updatedEntity = entity.copy(syncStatus = "synced")
                    notificationDao.updateNotification(updatedEntity)
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }

    override suspend fun deleteNotification(notification: Notification) {
        withContext(Dispatchers.IO) {
            val entity = notification.toEntity()
            notificationDao.deleteNotification(entity)

            if (networkUtils.isNetworkAvailable() && entity.serverId != null) {
                try {
                    apiService.deleteNotification(entity.serverId!!)
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }

    override suspend fun syncNotifications() {
        withContext(Dispatchers.IO) {
            if (!networkUtils.isNetworkAvailable()) return@withContext

            // Sync pending notifications
            val pendingNotifications = notificationDao.getPendingNotificationsSync()
            for (notification in pendingNotifications) {
                try {
                    when (notification.syncStatus) {
                        "pending" -> {
                            val response = apiService.createNotification(notification.toApiRequest())
                            val updatedNotification = notification.copy(
                                serverId = response.id.toString(),
                                syncStatus = "synced"
                            )
                            notificationDao.updateNotification(updatedNotification)
                        }
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }

            // Sync from server
            try {
                val serverNotifications = apiService.getNotifications()
                for (serverNotification in serverNotifications) {
                    val existingNotification = notificationDao.getNotificationByServerIdSync(serverNotification.id.toString())
                    if (existingNotification == null) {
                        notificationDao.insertNotification(serverNotification.toEntity())
                    } else if (serverNotification.updatedAt > existingNotification.lastModified) {
                        notificationDao.updateNotification(serverNotification.toEntity(existingNotification))
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    override suspend fun syncNotificationsByUser(userId: String) {
        withContext(Dispatchers.IO) {
            if (!networkUtils.isNetworkAvailable()) return@withContext

            // Sync pending notifications for user
            val pendingNotifications = notificationDao.getPendingNotificationsByUserSync(userId)
            for (notification in pendingNotifications) {
                try {
                    when (notification.syncStatus) {
                        "pending" -> {
                            val response = apiService.createNotification(notification.toApiRequest())
                            val updatedNotification = notification.copy(
                                serverId = response.id.toString(),
                                syncStatus = "synced"
                            )
                            notificationDao.updateNotification(updatedNotification)
                        }
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }

            // Sync from server for user
            try {
                val serverNotifications = apiService.getNotificationsByUser(userId)
                for (serverNotification in serverNotifications) {
                    val existingNotification = notificationDao.getNotificationByServerIdSync(serverNotification.id.toString())
                    if (existingNotification == null) {
                        notificationDao.insertNotification(serverNotification.toEntity())
                    } else if (serverNotification.updatedAt > existingNotification.lastModified) {
                        notificationDao.updateNotification(serverNotification.toEntity(existingNotification))
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
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
