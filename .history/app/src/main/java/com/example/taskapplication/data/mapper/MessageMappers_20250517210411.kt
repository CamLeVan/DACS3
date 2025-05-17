package com.example.taskapplication.data.mapper

import com.example.taskapplication.data.api.request.MessageRequest
import com.example.taskapplication.data.api.response.MessageResponse
import com.example.taskapplication.data.api.response.ReactionResponse
import com.example.taskapplication.data.database.entities.MessageEntity
import com.example.taskapplication.data.database.entities.MessageReactionEntity
import com.example.taskapplication.data.database.entities.MessageReadStatusEntity
import com.example.taskapplication.domain.model.Message
import com.example.taskapplication.domain.model.MessageReaction
import com.example.taskapplication.domain.model.MessageReadStatus
import java.util.*

// Entity to Domain
fun MessageEntity.toDomainModel(
    readBy: List<String> = emptyList(),
    reactions: List<MessageReaction> = emptyList(),
    attachments: List<com.example.taskapplication.domain.model.Attachment> = emptyList()
): Message {
    return Message(
        id = id,
        teamId = teamId,
        senderId = senderId,
        receiverId = receiverId,
        content = content,
        timestamp = timestamp,
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = lastModified,
        createdAt = createdAt,
        readBy = readBy,
        reactions = reactions,
        isDeleted = isDeleted,
        isRead = isRead,
        clientTempId = clientTempId,
        attachments = attachments
    )
}

// Domain to Entity
fun Message.toEntity(): MessageEntity {
    return MessageEntity(
        id = id,
        teamId = teamId,
        senderId = senderId,
        receiverId = receiverId,
        content = content,
        timestamp = timestamp,
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = lastModified,
        createdAt = createdAt,
        isDeleted = isDeleted,
        isRead = isRead,
        clientTempId = clientTempId
    )
}

// API Response to Entity
fun ChatMessageResponse.toEntity(existingMessage: MessageEntity? = null): MessageEntity {
    return MessageEntity(
        id = existingMessage?.id ?: UUID.randomUUID().toString(),
        teamId = teamId?.toString(),
        senderId = senderId.toString(),
        receiverId = receiverId?.toString(),
        content = content,
        timestamp = timestamp,
        serverId = id.toString(),
        syncStatus = "synced",
        lastModified = lastModified,
        createdAt = existingMessage?.createdAt ?: System.currentTimeMillis(),
        isDeleted = deletedAt != null,
        isRead = readBy.any { it.toString() == senderId.toString() },
        clientTempId = clientTempId ?: existingMessage?.clientTempId
    )
}

// Reaction Entity to Domain
fun MessageReactionEntity.toDomainModel(): MessageReaction {
    return MessageReaction(
        id = id,
        messageId = messageId,
        userId = userId,
        reaction = reaction ?: "",
        serverId = serverId,
        lastModified = lastModified
    )
}

// Domain Reaction to Entity
fun MessageReaction.toEntity(): MessageReactionEntity {
    return MessageReactionEntity(
        id = id,
        messageId = messageId,
        userId = userId,
        reaction = reaction,
        serverId = serverId,
        lastModified = lastModified
    )
}

// API Reaction Response to Entity
fun ReactionResponse.toEntity(existingReaction: MessageReactionEntity? = null): MessageReactionEntity {
    return MessageReactionEntity(
        id = existingReaction?.id ?: UUID.randomUUID().toString(),
        messageId = messageId.toString(),
        userId = userId.toString(),
        reaction = reaction,
        serverId = id.toString(),
        lastModified = createdAt
    )
}

// ReadStatus Entity to Domain
fun MessageReadStatusEntity.toDomainModel(): MessageReadStatus {
    return MessageReadStatus(
        id = id,
        messageId = messageId,
        userId = userId,
        readAt = readAt,
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = lastModified
    )
}

// Domain ReadStatus to Entity
fun MessageReadStatus.toEntity(): MessageReadStatusEntity {
    return MessageReadStatusEntity(
        id = id,
        messageId = messageId,
        userId = userId,
        readAt = readAt,
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = lastModified
    )
}

// Entity to API Request
fun MessageEntity.toApiRequest(): MessageRequest {
    return MessageRequest(
        content = content,
        teamId = teamId,
        receiverId = receiverId,
        senderId = senderId,
        clientTempId = clientTempId
    )
}

// Domain to API Request
fun Message.toApiRequest(): MessageRequest {
    return MessageRequest(
        content = content,
        teamId = teamId,
        receiverId = receiverId,
        senderId = senderId,
        clientTempId = clientTempId
    )
}