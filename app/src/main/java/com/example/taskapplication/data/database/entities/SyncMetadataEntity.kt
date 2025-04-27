package com.example.taskapplication.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val id: String, // Use a fixed ID like "global" or entity-specific IDs
    val lastSyncTimestamp: Long, // Last time sync was performed
    val entityType: String, // To track different entity types (e.g., "tasks", "messages")
    val extraData: String? = null // JSON string for any additional sync data
) 