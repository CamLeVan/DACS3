package com.example.taskapplication.domain.model

data class Notification(
    val id: String,
    val serverId: String,
    val type: String, // "new_message", "task_assignment", etc.
    val title: String,
    val body: String,
    val data: Map<String, String>,
    val isRead: Boolean,
    val createdAt: Long,
    val syncStatus: String,
    val lastModified: Long
)
