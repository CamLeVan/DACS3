package com.example.taskapplication.data.api.request

data class TeamTaskRequest(
    val title: String,
    val description: String? = null,
    val dueDate: Long? = null,
    val priority: Int,
    val isCompleted: Boolean,
    val assignedUserId: String? = null
)
