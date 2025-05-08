package com.example.taskapplication.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val username: String,
    val email: String,
    val fullName: String,
    val avatarUrl: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: String = "synced"
) 