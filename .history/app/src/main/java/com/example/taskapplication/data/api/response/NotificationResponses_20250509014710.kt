package com.example.taskapplication.data.api.response

// This file is kept for reference but the actual implementation is in NotificationSettingsResponse.kt

data class QuietHoursSettings(
    val enabled: Boolean,
    val start: String,
    val end: String
)