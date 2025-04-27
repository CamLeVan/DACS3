package com.example.taskapplication.domain.repository

import com.example.taskapplication.domain.model.Team
import com.example.taskapplication.domain.model.User
import kotlinx.coroutines.flow.Flow

interface TeamRepository {
    fun getAllTeams(): Flow<List<Team>>
    fun getTeamsByOwner(ownerId: String): Flow<List<Team>>
    suspend fun getTeamById(id: String): Team?
    suspend fun createTeam(team: Team): Result<Team>
    suspend fun updateTeam(team: Team): Result<Team>
    suspend fun deleteTeam(teamId: String): Result<Unit>
    suspend fun getTeamMembers(teamId: String): Result<List<User>>
    suspend fun addTeamMember(teamId: String, userId: String, role: String = "member"): Result<Unit>
    suspend fun removeTeamMember(teamId: String, userId: String): Result<Unit>
    suspend fun syncTeams(): Result<Unit>
} 