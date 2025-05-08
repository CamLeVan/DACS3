package com.example.taskapplication.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personal_tasks")
data class PersonalTaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val dueDate: Long,
    val priority: String,
    val status: String,
    val userId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: String = "synced"
) 