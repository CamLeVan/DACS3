package com.example.taskapplication.data.api.request

data class TeamRequest(
    val name: String,
    val description: String? = null,
    val members: List<Long>? = null
)

data class AddTeamMemberRequest(
    val user_id: Long
) 