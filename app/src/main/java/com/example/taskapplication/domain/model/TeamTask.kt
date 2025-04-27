package com.example.taskapplication.domain.model

data class TeamTask(
    val id: String,
    val teamId: String,
    val title: String,
    val description: String? = null,
    val assignedUserId: String? = null,
    val dueDate: Long? = null,
    val priority: Int,
    val isCompleted: Boolean,
    val serverId: Long? = null,
    val syncStatus: String = "synced",
    val lastModified: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
) 