package com.example.taskapplication.data.remote.dto

data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)

data class MessageResponse(
    val message: String
)

data class SyncResponse(
    val conflicts: List<DocumentConflict>,
    val changes: List<DocumentChange>
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