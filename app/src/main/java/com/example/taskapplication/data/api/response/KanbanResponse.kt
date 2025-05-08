package com.example.taskapplication.data.api.response

/**
 * Response model for kanban board
 */
data class KanbanResponse(
    val id: String,
    val name: String,
    val columns: List<KanbanColumnResponse>
)

/**
 * Response model for kanban column
 */
data class KanbanColumnResponse(
    val id: String,
    val name: String,
    val order: Int,
    val tasks: List<KanbanTaskResponse>
)

/**
 * Response model for kanban task
 */
data class KanbanTaskResponse(
    val id: String,
    val title: String,
    val description: String,
    val priority: String,
    val due_date: String?,
    val assigned_to: KanbanUserResponse?,
    val position: Int
)

/**
 * Response model for kanban user
 */
data class KanbanUserResponse(
    val id: String,
    val name: String,
    val avatar: String?
)
