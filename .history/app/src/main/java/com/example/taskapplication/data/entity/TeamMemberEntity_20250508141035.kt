package com.example.taskapplication.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "team_members")
data class TeamMemberEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val teamId: String,
    val userId: String,
    val role: String,
    val joinedAt: Long,
    val syncStatus: String = "synced"
) 