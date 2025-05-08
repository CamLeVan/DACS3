package com.example.taskapplication.domain.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val avatar: String? = null,
    val serverId: String? = null,
    val googleId: String? = null,
    val syncStatus: String = "synced",
    val lastModified: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)