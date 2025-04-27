package com.example.taskapplication.data.api.request

data class PersonalTaskRequest(
    val title: String,
    val description: String? = null,
    val dueDate: Long? = null,
    val priority: Int,
    val isCompleted: Boolean
)

data class TeamTaskRequest(
    val title: String,
    val description: String? = null,
    val assignedUserId: Long? = null,
    val dueDate: Long? = null,
    val priority: Int,
    val isCompleted: Boolean
) 