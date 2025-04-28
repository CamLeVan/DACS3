package com.example.taskapplication.data.api.request

data class LoginRequest(
    val email: String,
    val password: String,
    val device_id: String? = null
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val password_confirmation: String,
    val device_id: String? = null
)

data class LogoutRequest(
    val deviceId: String? = null
)