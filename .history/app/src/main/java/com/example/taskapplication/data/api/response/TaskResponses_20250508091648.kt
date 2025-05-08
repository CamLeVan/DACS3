package com.example.taskapplication.data.api.response

data class PaginatedResponse<T>(
    val data: List<T>,
    val links: PaginationLinks,
    val meta: PaginationMeta
)

data class PaginationLinks(
    val first: String?,
    val last: String?,
    val prev: String?,
    val next: String?
)

data class PaginationMeta(
    val current_page: Int,
    val from: Int,
    val last_page: Int,
    val path: String,
    val per_page: Int,
    val to: Int,
    val total: Int
)

data class PersonalTaskResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val status: String,
    val priority: String,
    val due_date: String,
    val created_at: String,
    val updated_at: String,
    val user_id: Long,
    val subtasks: List<SubtaskResponse>
)

data class TeamTaskResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val status: String,
    val priority: String,
    val due_date: String,
    val created_at: String,
    val updated_at: String,
    val team_id: Long,
    val assigned_to: List<UserResponse>,
    val subtasks: List<SubtaskResponse>
)

data class SubtaskResponse(
    val id: Long,
    val task_id: Long,
    val title: String,
    val is_completed: Boolean,
    val created_at: String,
    val updated_at: String
)

data class TaskMoveResponse(
    val id: Long,
    val title: String,
    val column_id: Long,
    val position: Int,
    val updated_at: String
) 