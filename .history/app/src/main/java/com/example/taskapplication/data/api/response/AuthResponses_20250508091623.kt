package com.example.taskapplication.data.api.response

data class AuthResponse(
    val user: UserResponse,
    val token: String,
    val device_id: String,
    val is_new_user: Boolean? = null
)

data class UserResponse(
    val id: Long,
    val name: String,
    val email: String,
    val avatar: String? = null,
    val google_id: String? = null,
    val settings: UserSettings? = null
)

data class UserSettings(
    val theme: String,
    val language: String,
    val notifications_enabled: Boolean
) 