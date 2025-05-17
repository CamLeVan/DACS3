package com.example.taskapplication.data.util

import android.content.Context
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cung cấp thông tin về thiết bị
 */
@Singleton
class DeviceInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStoreManager: DataStoreManager
) {
    /**
     * Lấy tên thiết bị
     * @return Tên thiết bị dưới dạng "Manufacturer Model"
     */
    fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.startsWith(manufacturer)) {
            model
        } else {
            "$manufacturer $model"
        }
    }

    /**
     * Lấy ID duy nhất của thiết bị
     */
    suspend fun getDeviceId(): String {
        // Kiểm tra xem đã có device ID trong DataStore chưa
        val savedDeviceId = dataStoreManager.getDeviceId()

        if (!savedDeviceId.isNullOrEmpty()) {
            return savedDeviceId
        }

        // Nếu chưa có, tạo mới và lưu vào DataStore
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )

        val deviceId = if (androidId != "9774d56d682e549c" && androidId.isNotEmpty()) {
            // Sử dụng Android ID nếu hợp lệ
            androidId
        } else {
            // Fallback: tạo UUID ngẫu nhiên
            UUID.randomUUID().toString()
        }

        // Lưu vào DataStore
        dataStoreManager.saveDeviceId(deviceId)

        return deviceId
    }

    /**
     * Lấy thông tin về thiết bị
     */
    fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            osVersion = Build.VERSION.RELEASE,
            sdkVersion = Build.VERSION.SDK_INT
        )
    }
}

/**
 * Thông tin về thiết bị
 */
data class DeviceInfo(
    val model: String,
    val manufacturer: String,
    val osVersion: String,
    val sdkVersion: Int
)
