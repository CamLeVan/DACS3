package com.example.taskapplication.domain.model



/**
 * Domain model for documents
 */
data class Document(
    val id: String,
    val name: String,
    val description: String?,
    val teamId: String,
    val folderId: String?,
    val fileUrl: String,
    val fileType: String,
    val fileSize: Long,
    val uploadedBy: String,
    val uploadedAt: Long,
    val lastModified: Long,
    val accessLevel: String, // public, team, private, specific_users
    val allowedUsers: List<String> = emptyList(),
    val syncStatus: String = "synced",
    val isDeleted: Boolean = false,
    val serverId: String? = null,
    val latestVersionId: String? = null,
    val thumbnailUrl: String? = null,

    // Helper methods
    val versions: List<DocumentVersion> = emptyList()
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

    /**
     * Check if user has access to this document
     */
    fun hasAccess(userId: String): Boolean {
        return when (accessLevel) {
            "public", "team" -> true
            "private" -> uploadedBy == userId
            "specific_users" -> uploadedBy == userId || userId in allowedUsers
            else -> false
        }
    }
}
