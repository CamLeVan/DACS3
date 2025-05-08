package com.example.taskapplication.data.api.request

/**
 * Request model for quiet hours settings
 */
data class QuietHoursSettings(
    val enabled: Boolean,
    val start: String,
    val end: String
)
