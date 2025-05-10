package com.example.taskapplication.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for storing documents
 */
@Entity(
    tableName = "documents",
    foreignKeys = [
        ForeignKey(
            entity = TeamEntity::class,
            parentColumns = ["id"],
            childColumns = ["teamId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DocumentFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("teamId"),
        Index("folderId"),
        Index("uploadedBy")
    ]
)
data class DocumentEntity(
    @PrimaryKey val id: String,
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
    val allowedUsers: String, // Comma-separated list of user IDs
    val syncStatus: String = "pending",
    val isDeleted: Boolean = false,
    val serverId: String? = null,
    val latestVersionId: String? = null,
    val thumbnailUrl: String? = null
)
