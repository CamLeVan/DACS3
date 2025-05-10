package com.example.taskapplication.data.mapper

import com.example.taskapplication.data.database.entities.TeamDocumentEntity
import com.example.taskapplication.domain.model.TeamDocument

/**
 * Convert TeamDocumentEntity to TeamDocument
 */
fun TeamDocumentEntity.toDomainModel(): TeamDocument {
    return TeamDocument(
        id = id.toString(),
        teamId = teamId.toString(),
        name = name,
        description = description ?: "",
        fileUrl = fileUrl,
        thumbnailUrl = thumbnailUrl,
        fileType = fileType,
        fileSize = fileSize,
        folderId = folderId?.toString(),
        uploadedBy = uploadedBy.toString(),
        uploadedByName = null, // Will be populated later if needed
        uploadedAt = uploadedAt,
        lastModified = lastModified,
        serverId = serverId?.toString(),
        syncStatus = syncStatus,
        accessLevel = accessLevel,
        allowedUsers = if (allowedUsers.isBlank()) emptyList() else allowedUsers.split(","),
        isDeleted = isDeleted == 1
    )
}

/**
 * Convert TeamDocument to TeamDocumentEntity
 */
fun TeamDocument.toEntity(): TeamDocumentEntity {
    return TeamDocumentEntity(
        id = id.toLongOrNull() ?: 0L,
        teamId = teamId.toLongOrNull() ?: 0L,
        name = name,
        description = description,
        fileUrl = fileUrl,
        thumbnailUrl = thumbnailUrl,
        fileType = fileType,
        fileSize = fileSize,
        folderId = folderId?.toLongOrNull(),
        uploadedBy = uploadedBy.toLongOrNull() ?: 0L,
        uploadedAt = uploadedAt,
        lastModified = lastModified,
        serverId = serverId?.toLongOrNull(),
        syncStatus = syncStatus,
        accessLevel = accessLevel,
        allowedUsers = allowedUsers.joinToString(","),
        isDeleted = if (isDeleted) 1 else 0
    )
}
