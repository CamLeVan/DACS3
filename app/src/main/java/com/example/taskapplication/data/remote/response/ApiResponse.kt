package com.example.taskapplication.data.remote.response

/**
 * Generic API response wrapper
 */
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T
)
