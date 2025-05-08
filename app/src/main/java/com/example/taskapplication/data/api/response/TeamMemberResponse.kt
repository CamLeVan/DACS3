package com.example.taskapplication.data.api.response

data class TeamMemberResponse(
    val id: String,
    val teamId: String,
    val userId: String,
    val role: String,
    val user: UserResponse
)
