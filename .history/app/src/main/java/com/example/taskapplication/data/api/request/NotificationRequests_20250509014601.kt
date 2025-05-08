package com.example.taskapplication.data.api.request

data class QuietHoursSettings(
    val enabled: Boolean,
    val start: String,
    val end: String
)