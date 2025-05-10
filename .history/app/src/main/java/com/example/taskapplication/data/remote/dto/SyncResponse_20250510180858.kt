package com.example.taskapplication.data.remote.dto

/**
 * Response for sync operations
 */
data class SyncResponse(
    val documents: List<DocumentDto>,
    val folders: List<FolderDto>,
    val versions: List<VersionDto>,
    val permissions: List<DocumentAccessDto>,
    val deletedDocuments: List<String>,
    val deletedFolders: List<String>,
    val deletedVersions: List<String>,
    val deletedPermissions: List<String>,
    val conflicts: List<ConflictDto>
)

/**
 * DTO for conflicts
 */
data class ConflictDto(
    val documentId: String,
    val localVersion: String,
    val serverVersion: String,
    val type: ConflictType
)

/**
 * Enum for conflict types
 */
enum class ConflictType {
    CONTENT_CONFLICT,
    METADATA_CONFLICT,
    PERMISSION_CONFLICT
}

/**
 * Simple message response
 */
data class MessageResponse(
    val message: String
)
