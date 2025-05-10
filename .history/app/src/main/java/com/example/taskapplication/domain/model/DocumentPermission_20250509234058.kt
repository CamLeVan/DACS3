package com.example.taskapplication.domain.model

import java.util.Date

/**
 * Domain model for document user permissions
 */
data class DocumentPermission(
    val id: String,
    val documentId: String,
    val userId: String,
    val permissionType: String, // view, edit, admin
    val grantedBy: String,
    val grantedAt: Date,
    val syncStatus: String = "synced",
    val serverId: String? = null
)
