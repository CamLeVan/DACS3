package com.example.taskapplication.domain.model

/**
 * Model đại diện cho tệp đính kèm trong tin nhắn
 */
data class Attachment(
    val id: String,
    val messageId: String? = null,
    val fileName: String,
    val fileSize: Long,
    val fileType: String,
    val url: String,
    val serverId: String? = null,
    val syncStatus: String = "synced",
    val createdAt: Long = System.currentTimeMillis()
)
