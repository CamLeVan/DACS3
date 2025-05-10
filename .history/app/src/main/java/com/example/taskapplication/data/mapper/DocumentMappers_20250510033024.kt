package com.example.taskapplication.data.mapper

import com.example.taskapplication.data.database.entities.DocumentEntity
import com.example.taskapplication.data.database.entities.DocumentFolderEntity
import com.example.taskapplication.data.database.entities.DocumentPermissionEntity
import com.example.taskapplication.data.database.entities.DocumentVersionEntity
import com.example.taskapplication.domain.model.Document
import com.example.taskapplication.domain.model.DocumentFolder
import com.example.taskapplication.domain.model.DocumentPermission
import com.example.taskapplication.domain.model.DocumentVersion
import com.example.taskapplication.util.DateConverter

/**
 * Convert DocumentFolderEntity to DocumentFolder
 */
fun DocumentFolderEntity.toDomain(): DocumentFolder {
    return DocumentFolder(
        id = id,
        name = name,
        description = description,
        teamId = teamId,
        parentFolderId = parentFolderId,
        createdBy = createdBy,
        createdAt = Date(createdAt),
        updatedAt = Date(updatedAt),
        syncStatus = syncStatus,
        isDeleted = isDeleted,
        serverId = serverId
    )
}

/**
 * Convert DocumentFolder to DocumentFolderEntity
 */
fun DocumentFolder.toEntity(): DocumentFolderEntity {
    return DocumentFolderEntity(
        id = id,
        name = name,
        description = description,
        teamId = teamId,
        parentFolderId = parentFolderId,
        createdBy = createdBy,
        createdAt = createdAt.time,
        updatedAt = updatedAt.time,
        syncStatus = syncStatus,
        isDeleted = isDeleted,
        serverId = serverId
    )
}

/**
 * Convert DocumentEntity to Document
 */
fun DocumentEntity.toDomain(versions: List<DocumentVersion> = emptyList()): Document {
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
        uploadedAt = Date(uploadedAt),
        lastModified = Date(lastModified),
        accessLevel = accessLevel,
        allowedUsers = allowedUsers.split(",").filter { it.isNotBlank() },
        syncStatus = syncStatus,
        isDeleted = isDeleted,
        serverId = serverId,
        latestVersionId = latestVersionId,
        versions = versions
    )
}

/**
 * Convert Document to DocumentEntity
 */
fun Document.toEntity(): DocumentEntity {
    return DocumentEntity(
        id = id,
        name = name,
        description = description,
        teamId = teamId,
        folderId = folderId,
        fileUrl = fileUrl,
        fileType = fileType,
        fileSize = fileSize,
        uploadedBy = uploadedBy,
        uploadedAt = uploadedAt.time,
        lastModified = lastModified.time,
        accessLevel = accessLevel,
        allowedUsers = allowedUsers.joinToString(","),
        syncStatus = syncStatus,
        isDeleted = isDeleted,
        serverId = serverId,
        latestVersionId = latestVersionId
    )
}

/**
 * Convert DocumentVersionEntity to DocumentVersion
 */
fun DocumentVersionEntity.toDomain(): DocumentVersion {
    return DocumentVersion(
        id = id,
        documentId = documentId,
        versionNumber = versionNumber,
        fileUrl = fileUrl,
        fileSize = fileSize,
        uploadedBy = uploadedBy,
        uploadedAt = Date(uploadedAt),
        changeNotes = changeNotes,
        syncStatus = syncStatus,
        serverId = serverId
    )
}

/**
 * Convert DocumentVersion to DocumentVersionEntity
 */
fun DocumentVersion.toEntity(): DocumentVersionEntity {
    return DocumentVersionEntity(
        id = id,
        documentId = documentId,
        versionNumber = versionNumber,
        fileUrl = fileUrl,
        fileSize = fileSize,
        uploadedBy = uploadedBy,
        uploadedAt = uploadedAt.time,
        changeNotes = changeNotes,
        syncStatus = syncStatus,
        serverId = serverId
    )
}

/**
 * Convert DocumentPermissionEntity to DocumentPermission
 */
fun DocumentPermissionEntity.toDomain(): DocumentPermission {
    return DocumentPermission(
        id = id,
        documentId = documentId,
        userId = userId,
        permissionType = permissionType,
        grantedBy = grantedBy,
        grantedAt = Date(grantedAt),
        syncStatus = syncStatus,
        serverId = serverId
    )
}

/**
 * Convert DocumentPermission to DocumentPermissionEntity
 */
fun DocumentPermission.toEntity(): DocumentPermissionEntity {
    return DocumentPermissionEntity(
        id = id,
        documentId = documentId,
        userId = userId,
        permissionType = permissionType,
        grantedBy = grantedBy,
        grantedAt = grantedAt.time,
        syncStatus = syncStatus,
        serverId = serverId
    )
}
