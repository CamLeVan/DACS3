package com.example.taskapplication.data.api.response

data class TaskAssignmentResponse(
    val id: Long,
    val taskId: Long,
    val userId: Long,
    val userName: String,
    val userAvatar: String?,
    val role: String,
    val assignedAt: Long,
    val assignedBy: Long,
    val assignedByName: String
)
