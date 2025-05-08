package com.example.taskapplication.data.api.request

/**
 * Request model for moving a task
 */
data class MoveTaskRequest(
    val column_id: String,
    val position: Int
)
