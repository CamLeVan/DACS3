package com.example.taskapplication.data.api.response

data class TeamTaskResponse(
    val id: String,
    val teamId: String,
    val title: String,
    val description: String? = null,
    val assignedUserId: String? = null,
    val dueDate: Long? = null,
    val priority: Int,
    val isCompleted: Boolean,
    val lastModified: Long,
    val createdAt: Long
)
