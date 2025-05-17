package com.example.taskapplication.domain.model

data class PersonalTask(
    val id: String,
    val title: String,
    val description: String? = null,
    val dueDate: Long? = null,
    val priority: String = "medium", // "low", "medium", "high"
    val status: String = "pending", // "pending", "in_progress", "completed", "archived"
    val category: String? = null,
    val tags: List<String>? = null,
    val reminderDate: Long? = null,
    val serverId: String? = null,
    val syncStatus: String = "synced",
    val lastModified: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null
) {
    fun isCompleted(): Boolean {
        return status == "completed"
    }

    fun toApiRequest(): com.example.taskapplication.data.api.request.PersonalTaskRequest {
        return com.example.taskapplication.data.api.request.PersonalTaskRequest(
            title = title,
            description = description,
            due_date = dueDate?.let { java.time.Instant.ofEpochMilli(it).toString() },
            priority = priority,
            status = status,
            category = category,
            tags = tags,
            reminder_date = reminderDate?.let { java.time.Instant.ofEpochMilli(it).toString() }
        )
    }
}