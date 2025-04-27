package com.example.taskapplication.domain.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String? = null,
    val serverId: Long? = null,
    val syncStatus: String = "synced",
    val lastModified: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
) 