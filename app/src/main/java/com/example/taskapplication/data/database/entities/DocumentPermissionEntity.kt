package com.example.taskapplication.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for storing document user permissions
 */
@Entity(
    tableName = "document_permissions",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("documentId"),
        Index("userId")
    ]
)
data class DocumentPermissionEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val userId: String,
    val permissionType: String, // view, edit, admin
    val grantedBy: String,
    val grantedAt: Long,
    val syncStatus: String = "pending",
    val serverId: String? = null
)
