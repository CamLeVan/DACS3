package com.example.taskapplication.data.repository

import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.api.request.RegisterDeviceRequest
import com.example.taskapplication.data.mapper.toNotification
import com.example.taskapplication.domain.model.Notification
import com.example.taskapplication.domain.repository.NotificationRepository
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : NotificationRepository {
    
    override suspend fun getNotifications(since: Long?): Result<List<Notification>> {
        return try {
            val response = apiService.getNotifications(since)
            
            if (response.isSuccessful) {
                val notifications = response.body()?.map { it.toNotification() } ?: emptyList()
                Result.success(notifications)
            } else {
                Result.failure(Exception("Failed to fetch notifications: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun registerDevice(deviceId: String, fcmToken: String): Result<Unit> {
        return try {
            val request = RegisterDeviceRequest(
                device_id = deviceId,
                fcm_token = fcmToken,
                device_type = "android",
                device_name = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL
            )
            
            val response = apiService.registerDevice(request)
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to register device: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun unregisterDevice(deviceId: String): Result<Unit> {
        return try {
            val response = apiService.unregisterDevice(deviceId)
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to unregister device: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
