package com.example.taskapplication.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for storing document folders
 */
@Entity(
    tableName = "document_folders",
    foreignKeys = [
        ForeignKey(
            entity = TeamEntity::class,
            parentColumns = ["id"],
            childColumns = ["teamId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("teamId"),
        Index("parentFolderId"),
        Index("createdBy")
    ]
)
data class DocumentFolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val teamId: String,
    val parentFolderId: String?,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: String = "pending",
    val isDeleted: Boolean = false,
    val serverId: String? = null
)
