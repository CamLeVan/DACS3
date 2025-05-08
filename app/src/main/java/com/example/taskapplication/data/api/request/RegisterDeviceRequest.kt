package com.example.taskapplication.data.api.request

/**
 * Request model for registering a device for push notifications
 */
data class RegisterDeviceRequest(
    val device_id: String,
    val fcm_token: String,
    val device_type: String,
    val device_name: String
)
