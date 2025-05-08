package com.example.taskapplication.data.api.request

data class RegisterDeviceRequest(
    val deviceId: String,
    val deviceName: String,
    val fcmToken: String,
    val platform: String = "android"
)
