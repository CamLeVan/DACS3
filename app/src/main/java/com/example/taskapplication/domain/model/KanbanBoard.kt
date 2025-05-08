package com.example.taskapplication.domain.model

/**
 * Domain model for kanban board
 */
data class KanbanBoard(
    val id: String,
    val name: String,
    val teamId: String,
    val columns: List<KanbanColumn>,
    val serverId: String? = null,
    val syncStatus: String = "synced", // synced, pending_create, pending_update, pending_delete
    val lastModified: Long = System.currentTimeMillis()
)

/**
 * Domain model for kanban column
 */
data class KanbanColumn(
    val id: String,
    val name: String,
    val order: Int,
    val tasks: List<KanbanTask>,
    val serverId: String? = null,
    val syncStatus: String = "synced", // synced, pending_create, pending_update, pending_delete
    val lastModified: Long = System.currentTimeMillis()
)

/**
 * Domain model for kanban task
 */
data class KanbanTask(
    val id: String,
    val title: String,
    val description: String,
    val priority: String,
    val dueDate: Long?,
    val assignedTo: KanbanUser?,
    val position: Int,
    val serverId: String? = null,
    val syncStatus: String = "synced", // synced, pending_create, pending_update, pending_delete
    val lastModified: Long = System.currentTimeMillis()
)

/**
 * Domain model for kanban user
 */
data class KanbanUser(
    val id: String,
    val name: String,
    val avatar: String?
)
