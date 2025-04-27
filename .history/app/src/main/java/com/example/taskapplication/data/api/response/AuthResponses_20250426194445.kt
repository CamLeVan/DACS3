package com.example.taskapplication.data.api.response

data class AuthResponse(
    val token: String,
    val expiresAt: Long,
    val refreshToken: String?,
    val user: UserResponse
)

data class UserResponse(
    val id: Long,
    val name: String,
    val email: String,
    val avatarUrl: String?
) 