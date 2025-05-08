package com.example.taskapplication.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

// Placeholder: Define actual fields based on your Laravel migration
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String, // UUID String or server ID depending on your strategy
    val name: String,
    val email: String,
    val avatar: String?,
    val serverId: String,
    val syncStatus: String, // "synced", "pending_update"
    val lastModified: Long,
    val createdAt: Long
) 