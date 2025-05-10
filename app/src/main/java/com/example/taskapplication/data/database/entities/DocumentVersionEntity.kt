package com.example.taskapplication.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for storing document versions
 */
@Entity(
    tableName = "document_versions",
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
        Index("uploadedBy")
    ]
)
data class DocumentVersionEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val versionNumber: Int,
    val fileUrl: String,
    val fileSize: Long,
    val uploadedBy: String,
    val uploadedAt: Long,
    val changeNotes: String,
    val syncStatus: String = "pending",
    val serverId: String? = null
)
