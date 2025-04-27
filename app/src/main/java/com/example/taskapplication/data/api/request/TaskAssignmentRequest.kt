package com.example.taskapplication.data.api.request

data class TaskAssignmentRequest(
    val userId: Long,
    val role: String = "assignee" // Có thể là "assignee", "reviewer", etc.
)
