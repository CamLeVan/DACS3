package com.example.taskapplication.data.api.response

data class TeamResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val owner: UserResponse,
    val memberCount: Int,
    val createdAt: Long,
    val updatedAt: Long
)

data class TeamMemberResponse(
    val id: Long,
    val teamId: Long,
    val user: UserResponse,
    val role: String,
    val joinedAt: Long
) 