package com.example.taskapplication.domain.model

import java.util.Date

/**
 * Domain model for document versions
 */
data class DocumentVersion(
    val id: String,
    val documentId: String,
    val versionNumber: Int,
    val fileUrl: String,
    val fileSize: Long,
    val uploadedBy: String,
    val uploadedAt: Long,
    val changeNotes: String,
    val syncStatus: String = "synced",
    val serverId: String? = null
) {
    /**
     * Get formatted file size (KB, MB, GB)
     */
    fun getFormattedFileSize(): String {
        return when {
            fileSize < 1024 -> "$fileSize B"
            fileSize < 1024 * 1024 -> "${fileSize / 1024} KB"
            fileSize < 1024 * 1024 * 1024 -> "${fileSize / (1024 * 1024)} MB"
            else -> "${fileSize / (1024 * 1024 * 1024)} GB"
        }
    }
}
