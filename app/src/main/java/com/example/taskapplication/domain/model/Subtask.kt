package com.example.taskapplication.domain.model

data class Subtask(
    val id: String,
    val taskableType: String, // "App\\Models\\PersonalTask" or "App\\Models\\TeamTask"
    val taskableId: String, // ID of the parent task
    val title: String,
    val completed: Boolean = false,
    val order: Int = 0,
    val serverId: String? = null,
    val syncStatus: String = PersonalTask.SyncStatus.SYNCED,
    val lastModified: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null
) {
    // Subtask API đã bị loại bỏ
}
