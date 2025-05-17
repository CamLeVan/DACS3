package com.example.taskapplication.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for storing team role change history
 */
@Entity(
    tableName = "team_role_history",
    indices = [
        Index("teamId"),
        Index("userId"),
        Index("changedByUserId")
    ]
)
data class TeamRoleHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val teamId: String,
    val userId: String,
    val oldRole: String,
    val newRole: String,
    val changedByUserId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val syncStatus: String = "pending"
)