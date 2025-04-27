package com.example.taskapplication.data.api.request

data class TeamRequest(
    val name: String,
    val description: String? = null
)

data class AddTeamMemberRequest(
    val userId: Long,
    val role: String = "member" // e.g., "member", "admin"
) 