package com.example.taskapplication.data.api.request

data class LoginRequest(
    val email: String,
    val password: String,
    val deviceId: String? = null
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val passwordConfirmation: String,
    val deviceId: String? = null
)

data class LogoutRequest(
    val deviceId: String? = null
) 