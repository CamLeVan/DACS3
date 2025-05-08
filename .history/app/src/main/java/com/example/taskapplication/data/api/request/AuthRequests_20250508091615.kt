package com.example.taskapplication.data.api.request

data class LoginRequest(
    val email: String,
    val password: String,
    val device_id: String,
    val device_name: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val password_confirmation: String,
    val device_id: String,
    val device_name: String
)

data class GoogleAuthRequest(
    val id_token: String,
    val device_id: String,
    val device_name: String
)

data class LogoutRequest(
    val device_id: String
)

data class BiometricRegisterRequest(
    val device_id: String,
    val biometric_type: String,
    val public_key: String
)

data class BiometricVerifyRequest(
    val device_id: String,
    val biometric_type: String,
    val signature: String
)