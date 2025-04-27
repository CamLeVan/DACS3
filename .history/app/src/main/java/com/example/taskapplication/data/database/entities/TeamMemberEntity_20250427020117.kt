package com.example.taskapplication.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "team_members")
data class TeamMemberEntity(
    @PrimaryKey val id: String, // UUID String
    val teamId: String,
    val userId: String,
    val role: String, // "admin", "member", etc.
    val joinedAt: Long,
    val invitedBy: String? = null,
    val serverId: Long? = null,
    val syncStatus: String, // "synced", "pending_create", "pending_update", "pending_delete"
    val lastModified: Long,
    val createdAt: Long
)
