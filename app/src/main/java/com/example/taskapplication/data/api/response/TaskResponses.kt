package com.example.taskapplication.data.api.response

data class PersonalTaskResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val dueDate: Long?,
    val priority: Int,
    val isCompleted: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val labels: List<String>? = null,
    val reminderMinutesBefore: Int? = null
)

data class TeamTaskResponse(
    val id: Long,
    val teamId: Long,
    val title: String,
    val description: String?,
    val assignedUser: UserResponse?,
    val dueDate: Long?,
    val priority: Int,
    val isCompleted: Boolean,
    val createdAt: Long,
    val updatedAt: Long
) 