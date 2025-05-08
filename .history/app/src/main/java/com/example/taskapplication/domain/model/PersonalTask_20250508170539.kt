package com.example.taskapplication.domain.model

data class PersonalTask(
    val id: String,
    val title: String,
    val description: String? = null,
    val dueDate: Long? = null,
    val priority: Int,
    val isCompleted: Boolean,
    val serverId: String? = null,
    val syncStatus: String = "synced",
    val lastModified: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)