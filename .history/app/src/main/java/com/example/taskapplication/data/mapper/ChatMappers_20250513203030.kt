package com.example.taskapplication.data.mapper

import com.example.taskapplication.data.api.model.ApiAttachment
import com.example.taskapplication.data.api.model.ApiMessage
import com.example.taskapplication.data.api.model.ApiMessageDelete
import com.example.taskapplication.data.api.model.ApiMessageUpdate
import com.example.taskapplication.data.api.model.ApiReadStatus
import com.example.taskapplication.data.api.model.ApiReaction
import com.example.taskapplication.data.api.model.ApiTypingStatus
import com.example.taskapplication.data.database.entities.AttachmentEntity
import com.example.taskapplication.data.database.entities.MessageEntity
import com.example.taskapplication.data.database.entities.MessageReadStatusEntity
import com.example.taskapplication.data.database.entities.MessageReactionEntity
import com.example.taskapplication.domain.model.Attachment
import com.example.taskapplication.domain.model.Message
import com.example.taskapplication.domain.model.MessageReaction
import com.example.taskapplication.domain.model.MessageReadStatus
import com.example.taskapplication.domain.model.User
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

// API to Domain
fun ApiMessage.toDomainModel(): Message {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())

    return Message(
        id = id.toString(),
        teamId = teamId.toString(),
        senderId = userId.toString(),
        content = message,
        timestamp = try {
            dateFormat.parse(createdAt)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        },
        serverId = id.toString(),
        syncStatus = "synced",
        lastModified = try {
            dateFormat.parse(updatedAt)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        },
        createdAt = try {
            dateFormat.parse(createdAt)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        },
        readBy = readStatus.map { it.userId.toString() },
        reactions = reactions.map { it.toDomainModel() },
        senderName = user.name,
        isDeleted = deletedAt != null,
        isRead = readStatus.any { it.userId.toString() != userId.toString() },
        clientTempId = clientTempId,
        attachments = attachments.map { it.toDomainModel() }
    )
}

fun ApiAttachment.toDomainModel(): Attachment {
    return Attachment(
        id = id.toString(),
        fileName = fileName,
        fileSize = fileSize,
        fileType = fileType,
        url = url,
        messageId = null, // Cần cập nhật khi gắn với tin nhắn
        syncStatus = "synced"
    )
}

fun ApiReaction.toDomainModel(): MessageReaction {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())

    return MessageReaction(
        id = UUID.randomUUID().toString(), // API không trả về ID cho reaction
        messageId = messageId.toString(),
        userId = userId.toString(),
        reaction = reaction,
        serverId = null,
        timestamp = try {
            dateFormat.parse(createdAt)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    )
}

fun ApiReadStatus.toDomainModel(): MessageReadStatus {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())

    return MessageReadStatus(
        id = UUID.randomUUID().toString(), // API không trả về ID cho read status
        messageId = messageId.toString(),
        userId = userId.toString(),
        readAt = try {
            dateFormat.parse(readAt)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        },
        serverId = null,
        syncStatus = "synced"
    )
}

// Domain to Entity
fun Message.toEntity(): MessageEntity {
    return MessageEntity(
        id = id,
        content = content,
        senderId = senderId,
        teamId = teamId,
        receiverId = null,
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

fun Attachment.toEntity(): AttachmentEntity {
    return AttachmentEntity(
        id = id,
        fileName = fileName,
        fileSize = fileSize,
        mimeType = fileType,
        fileUrl = url,
        filePath = url, // Sử dụng URL làm đường dẫn tệp mặc định
        messageId = messageId ?: "",
        syncStatus = syncStatus,
        type = when {
            fileType.startsWith("image/") -> "image"
            fileType.startsWith("video/") -> "video"
            fileType.startsWith("audio/") -> "audio"
            else -> "document"
        }
    )
}

fun MessageReaction.toEntity(): MessageReactionEntity {
    return MessageReactionEntity(
        id = id,
        messageId = messageId,
        userId = userId,
        reaction = reaction,
        serverId = serverId,
        timestamp = timestamp
    )
}

fun MessageReadStatus.toEntity(): MessageReadStatusEntity {
    return MessageReadStatusEntity(
        id = id,
        messageId = messageId,
        userId = userId,
        readAt = readAt,
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = System.currentTimeMillis()
    )
}
