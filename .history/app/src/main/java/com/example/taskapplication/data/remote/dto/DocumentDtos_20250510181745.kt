package com.example.taskapplication.data.remote.dto

import com.example.taskapplication.domain.model.Document
import com.example.taskapplication.domain.model.DocumentFolder
import com.example.taskapplication.domain.model.DocumentPermission
import com.example.taskapplication.domain.model.DocumentVersion
import com.google.gson.annotations.SerializedName


/**
 * DTO for document folders
 */
data class DocumentFolderDto(
    val id: Long,
    val name: String,
    val description: String?,
    @SerializedName("team_id")
    val teamId: String,
    @SerializedName("parent_id")
    val parentId: Long?,
    @SerializedName("created_by")
    val createdBy: String,
    @SerializedName("created_at")
    val createdAt: Long,
    @SerializedName("updated_at")
    val updatedAt: Long
)

/**
 * DTO for documents
 */
data class DocumentDto(
    val id: Long,
    val name: String,
    val description: String?,
    @SerializedName("team_id")
    val teamId: String,
    @SerializedName("folder_id")
    val folderId: Long?,
    @SerializedName("file_url")
    val fileUrl: String,
    @SerializedName("file_type")
    val fileType: String,
    @SerializedName("file_size")
    val fileSize: Long,
    @SerializedName("uploaded_by")
    val uploadedBy: String,
    @SerializedName("uploaded_at")
    val uploadedAt: Long,
    @SerializedName("last_modified")
    val lastModified: Long,
    @SerializedName("access_level")
    val accessLevel: String,
    @SerializedName("allowed_users")
    val allowedUsers: List<String>,
    @SerializedName("latest_version_id")
    val latestVersionId: Long?,
    @SerializedName("thumbnail_url")
    val thumbnailUrl: String?,
    val versions: List<DocumentVersionDto>? = null
)

/**
 * DTO for document versions
 */
data class DocumentVersionDto(
    val id: Long,
    @SerializedName("document_id")
    val documentId: Long,
    @SerializedName("version_number")
    val versionNumber: Int,
    @SerializedName("file_url")
    val fileUrl: String,
    @SerializedName("file_size")
    val fileSize: Long,
    @SerializedName("uploaded_by")
    val uploadedBy: String,
    @SerializedName("uploaded_at")
    val uploadedAt: Long,
    @SerializedName("change_notes")
    val changeNotes: String?
)

/**
 * DTO for document permissions
 */
data class DocumentPermissionDto(
    val id: String,
    @SerializedName("document_id")
    val documentId: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("permission_type")
    val permissionType: String,
    @SerializedName("granted_by")
    val grantedBy: String,
    @SerializedName("granted_at")
    val grantedAt: Long
)

/**
 * Extension function to convert DocumentFolderDto to DocumentFolder
 */
fun DocumentFolderDto.toDomain(): DocumentFolder {
    return DocumentFolder(
        id = id,
        name = name,
        description = description,
        teamId = teamId,
        parentFolderId = parentFolderId,
        createdBy = createdBy,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncStatus = "synced",
        serverId = id
    )
}

/**
 * Extension function to convert DocumentDto to Document
 */
fun DocumentDto.toDomain(): Document {
    return Document(
        id = id,
        name = name,
        description = description,
        teamId = teamId,
        folderId = folderId,
        fileUrl = fileUrl,
        fileType = fileType,
        fileSize = fileSize,
        uploadedBy = uploadedBy,
        uploadedAt = uploadedAt,
        lastModified = lastModified,
        accessLevel = accessLevel,
        allowedUsers = allowedUsers,
        syncStatus = "synced",
        serverId = id,
        latestVersionId = latestVersionId,
        versions = versions?.map { it.toDomain() } ?: emptyList()
    )
}

/**
 * Extension function to convert DocumentVersionDto to DocumentVersion
 */
fun DocumentVersionDto.toDomain(): DocumentVersion {
    return DocumentVersion(
        id = id,
        documentId = documentId,
        versionNumber = versionNumber,
        fileUrl = fileUrl,
        fileSize = fileSize,
        uploadedBy = uploadedBy,
        uploadedAt = uploadedAt,
        changeNotes = changeNotes,
        syncStatus = "synced",
        serverId = id
    )
}

/**
 * Extension function to convert DocumentPermissionDto to DocumentPermission
 */
fun DocumentPermissionDto.toDomain(): DocumentPermission {
    return DocumentPermission(
        id = id,
        documentId = documentId,
        userId = userId,
        permissionType = permissionType,
        grantedBy = grantedBy,
        grantedAt = grantedAt,
        syncStatus = "synced",
        serverId = id
    )
}
