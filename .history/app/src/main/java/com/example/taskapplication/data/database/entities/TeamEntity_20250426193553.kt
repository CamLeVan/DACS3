package com.example.taskapplication.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

// Placeholder: Define actual fields based on your requirements
@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey val id: String, // UUID String
    val name: String,
    val description: String?,
    val ownerId: String,
    val serverId: Long?,
    val syncStatus: String,
    val lastModified: Long,
    val createdAt: Long
    // Consider adding members list relation or a separate join table
) 