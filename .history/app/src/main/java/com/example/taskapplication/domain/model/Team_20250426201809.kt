package com.example.taskapplication.domain.model

data class Team(
    val id: String,
    val name: String,
    val description: String? = null,
    val ownerId: String,
    val serverId: Long? = null,
    val syncStatus: String = "synced",
    val lastModified: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
) 