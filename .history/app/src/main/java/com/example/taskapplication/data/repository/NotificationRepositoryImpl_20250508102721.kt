package com.example.taskapplication.data.repository

import android.util.Log
import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.api.request.RegisterDeviceRequest
import com.example.taskapplication.data.mapper.toNotification
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.domain.model.Notification
import com.example.taskapplication.domain.repository.NotificationRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val connectionChecker: ConnectionChecker,
    private val dataStoreManager: DataStoreManager
) : NotificationRepository {

    private val TAG = "NotificationRepository"
    
    override suspend fun getNotifications(since: Long?): Result<List<Notification>> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        return try {
            val response = apiService.getNotifications(since)
            
            if (response.isSuccessful) {
                val notifications = response.body()?.map { it.toNotification() } ?: emptyList()
                Result.success(notifications)
            } else {
                Log.e(TAG, "Failed to fetch notifications: ${response.code()} ${response.message()}")
                Result.failure(Exception("Failed to fetch notifications: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching notifications", e)
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
                deviceId = deviceId,
                fcmToken = fcmToken,
                deviceType = "android",
                deviceName = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL,
                userId = currentUserId
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
