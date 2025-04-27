package com.example.taskapplication.data.api.response

data class NotificationResponse(
    val id: Long,
    val type: String, // "new_message", "task_assignment", etc.
    val title: String,
    val body: String,
    val data: Map<String, Any>,
    val isRead: Boolean,
    val createdAt: Long
)
