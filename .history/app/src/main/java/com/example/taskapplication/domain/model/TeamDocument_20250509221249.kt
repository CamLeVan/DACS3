package com.example.taskapplication.domain.model

/**
 * Domain model for team document
 */
data class TeamDocument(
    val id: String,
    val teamId: String,
    val name: String,
    val description: String,
    val fileUrl: String,
    val fileType: String, // pdf, doc, image, etc.
    val fileSize: Long, // in bytes
    val uploadedBy: String, // userId
    val uploadedByName: String? = null, // user name
    val uploadedAt: Long,
    val lastModified: Long,
    val serverId: String? = null,
    val syncStatus: String = "pending", // synced, pending, error
    val accessLevel: String = "team", // team, admin, specific_users
    val allowedUsers: List<String> = emptyList(), // list of userIds
    val isDeleted: Boolean = false
) {
    /**
     * Get formatted file size
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
     * Get file extension
     */
    fun getFileExtension(): String {
        return fileUrl.substringAfterLast(".", "")
    }
    
    /**
     * Check if user has access to this document
     */
    fun hasAccess(userId: String, isAdmin: Boolean): Boolean {
        return when (accessLevel) {
            "team" -> true
            "admin" -> isAdmin
            "specific_users" -> allowedUsers.contains(userId) || uploadedBy == userId || isAdmin
            else -> false
        }
    }
}
