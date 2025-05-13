package com.example.taskapplication.data.mapper

import com.example.taskapplication.data.api.model.ApiAttachment
import com.example.taskapplication.data.api.model.ApiMessage
import com.example.taskapplication.data.api.model.ApiReaction
import com.example.taskapplication.data.api.model.ApiReadStatus
import com.example.taskapplication.domain.model.Attachment
import com.example.taskapplication.domain.model.Message
import com.example.taskapplication.domain.model.MessageReaction
import com.example.taskapplication.domain.model.MessageReadStatus
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

/**
 * Mappers for API models to Domain models
 */

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
