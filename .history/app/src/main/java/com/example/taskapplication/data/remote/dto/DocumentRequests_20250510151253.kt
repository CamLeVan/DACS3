package com.example.taskapplication.data.remote.dto

data class UpdateDocumentRequest(
    val name: String,
    val description: String? = null,
    val version: String
)

data class UpdateDocumentAccessRequest(
    val userId: String,
    val permission: DocumentPermission
)

data class CreateFolderRequest(
    val name: String,
    val parentId: String? = null
)

data class UpdateFolderRequest(
    val name: String,
    val parentId: String? = null
)

data class ResolveConflictsRequest(
    val documentId: String,
    val version: String,
    val resolution: ConflictResolution
)

enum class ConflictResolution {
    KEEP_LOCAL,
    USE_SERVER,
    MERGE
} 