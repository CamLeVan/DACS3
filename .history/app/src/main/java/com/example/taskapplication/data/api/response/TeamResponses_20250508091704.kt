package com.example.taskapplication.data.api.response

data class TeamResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val created_at: String,
    val updated_at: String,
    val owner: UserResponse,
    val members_count: Int,
    val tasks_count: Int
)

data class TeamDetailResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val created_at: String,
    val updated_at: String,
    val owner: UserResponse,
    val members: List<TeamMemberResponse>
)

data class TeamMemberResponse(
    val id: Long,
    val name: String,
    val avatar: String?,
    val role: String
)

data class KanbanBoardResponse(
    val id: Long,
    val name: String,
    val columns: List<KanbanColumnResponse>
)

data class KanbanColumnResponse(
    val id: Long,
    val name: String,
    val order: Int,
    val tasks: List<TeamTaskResponse>
) 