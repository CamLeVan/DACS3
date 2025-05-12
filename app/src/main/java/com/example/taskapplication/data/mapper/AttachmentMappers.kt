package com.example.taskapplication.data.mapper

import com.example.taskapplication.data.database.entities.AttachmentEntity
import com.example.taskapplication.domain.model.Attachment
import java.util.UUID

/**
 * Chuyển đổi từ Entity sang Domain model
 */
fun AttachmentEntity.toDomainModel(): Attachment {
    return Attachment(
        id = id,
        messageId = messageId,
        fileName = fileName,
        fileSize = fileSize,
        fileType = fileType,
        url = url,
        serverId = serverId,
        syncStatus = syncStatus,
        createdAt = createdAt
    )
}

/**
 * Chuyển đổi từ Domain model sang Entity
 */
fun Attachment.toEntity(): AttachmentEntity {
    return AttachmentEntity(
        id = id,
        messageId = messageId,
        fileName = fileName,
        fileSize = fileSize,
        fileType = fileType,
        url = url,
        serverId = serverId,
        syncStatus = syncStatus,
        createdAt = createdAt
    )
}

/**
 * Chuyển đổi từ API response sang Entity
 */
fun com.example.taskapplication.data.api.response.AttachmentResponse.toEntity(
    existingAttachment: AttachmentEntity? = null
): AttachmentEntity {
    return AttachmentEntity(
        id = existingAttachment?.id ?: UUID.randomUUID().toString(),
        messageId = message_id?.toString(),
        fileName = file_name,
        fileSize = file_size,
        fileType = file_type,
        url = url,
        serverId = id.toString(),
        syncStatus = "synced",
        createdAt = created_at
    )
}

/**
 * Chuyển đổi từ Entity sang API request
 */
fun AttachmentEntity.toApiRequest(): com.example.taskapplication.data.api.request.AttachmentRequest {
    return com.example.taskapplication.data.api.request.AttachmentRequest(
        fileId = id,
        messageId = messageId
    )
}

/**
 * Chuyển đổi từ Domain model sang API request
 */
fun Attachment.toApiRequest(): com.example.taskapplication.data.api.request.AttachmentRequest {
    return com.example.taskapplication.data.api.request.AttachmentRequest(
        fileId = id,
        messageId = messageId
    )
}
