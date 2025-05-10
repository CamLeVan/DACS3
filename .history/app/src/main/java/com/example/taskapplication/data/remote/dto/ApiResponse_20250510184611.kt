package com.example.taskapplication.data.remote.dto

sealed class ApiResponse<out T> {
    data class Success<T>(val data: T) : ApiResponse<T>()
    data class Error(val message: String, val code: Int) : ApiResponse<Nothing>()
}

data class ApiMessageResponse(
    val message: String
)

data class DocumentConflict(
    val documentId: String,
    val localVersion: String,
    val serverVersion: String
)

data class DocumentChange(
    val documentId: String,
    val type: ChangeType,
    val version: String
)

enum class ChangeType {
    CREATED,
    UPDATED,
    DELETED
}