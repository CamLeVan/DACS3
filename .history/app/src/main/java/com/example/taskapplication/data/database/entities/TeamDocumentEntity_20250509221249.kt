package com.example.taskapplication.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for storing team documents
 */
@Entity(
    tableName = "team_documents",
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
        Index("uploadedBy")
    ]
)
data class TeamDocumentEntity(
    @PrimaryKey val id: String,
    val teamId: String,
    val name: String,
    val description: String,
    val fileUrl: String,
    val fileType: String, // pdf, doc, image, etc.
    val fileSize: Long, // in bytes
    val uploadedBy: String, // userId
    val uploadedAt: Long,
    val lastModified: Long,
    val serverId: String? = null,
    val syncStatus: String = "pending", // synced, pending, error
    val accessLevel: String = "team", // team, admin, specific_users
    val allowedUsers: String = "", // comma-separated list of userIds
    val isDeleted: Boolean = false
)
