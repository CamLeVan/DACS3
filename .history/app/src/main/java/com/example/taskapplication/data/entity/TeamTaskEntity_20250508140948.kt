package com.example.taskapplication.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "team_tasks")
data class TeamTaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val dueDate: Long,
    val priority: String,
    val status: String,
    val teamId: String,
    val assignedTo: String?,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: String = "synced"
) 