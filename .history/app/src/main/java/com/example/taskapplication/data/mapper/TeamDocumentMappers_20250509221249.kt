package com.example.taskapplication.data.mapper

import com.example.taskapplication.data.database.entities.TeamDocumentEntity
import com.example.taskapplication.domain.model.TeamDocument

/**
 * Convert TeamDocumentEntity to TeamDocument
 */
fun TeamDocumentEntity.toDomainModel(): TeamDocument {
    return TeamDocument(
        id = id,
        teamId = teamId,
        name = name,
        description = description,
        fileUrl = fileUrl,
        fileType = fileType,
        fileSize = fileSize,
        uploadedBy = uploadedBy,
        uploadedByName = null, // Will be populated later if needed
        uploadedAt = uploadedAt,
        lastModified = lastModified,
        serverId = serverId,
        syncStatus = syncStatus,
        accessLevel = accessLevel,
        allowedUsers = if (allowedUsers.isBlank()) emptyList() else allowedUsers.split(","),
        isDeleted = isDeleted
    )
}

/**
 * Convert TeamDocument to TeamDocumentEntity
 */
fun TeamDocument.toEntity(): TeamDocumentEntity {
    return TeamDocumentEntity(
        id = id,
        teamId = teamId,
        name = name,
        description = description,
        fileUrl = fileUrl,
        fileType = fileType,
        fileSize = fileSize,
        uploadedBy = uploadedBy,
        uploadedAt = uploadedAt,
        lastModified = lastModified,
        serverId = serverId,
        syncStatus = syncStatus,
        accessLevel = accessLevel,
        allowedUsers = allowedUsers.joinToString(","),
        isDeleted = isDeleted
    )
}
