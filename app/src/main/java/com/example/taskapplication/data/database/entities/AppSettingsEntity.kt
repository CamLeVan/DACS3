package com.example.taskapplication.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity để lưu trữ cài đặt ứng dụng
 */
@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1, // Chỉ có một bản ghi cài đặt
    val current_user_id: String? = null,
    val theme_mode: String = "system", // "light", "dark", "system"
    val notification_enabled: Boolean = true,
    val last_sync_timestamp: Long = 0
)
