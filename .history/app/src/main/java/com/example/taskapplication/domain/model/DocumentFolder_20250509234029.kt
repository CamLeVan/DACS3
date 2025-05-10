package com.example.taskapplication.domain.model

import java.util.Date

/**
 * Domain model for document folders
 */
data class DocumentFolder(
    val id: String,
    val name: String,
    val description: String,
    val teamId: String,
    val parentFolderId: String?,
    val createdBy: String,
    val createdAt: Date,
    val updatedAt: Date,
    val syncStatus: String = "synced",
    val isDeleted: Boolean = false,
    val serverId: String? = null
)
