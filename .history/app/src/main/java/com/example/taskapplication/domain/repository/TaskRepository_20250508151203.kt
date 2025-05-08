package com.example.taskapplication.domain.repository

import com.example.taskapplication.domain.model.PersonalTask
import com.example.taskapplication.domain.model.TeamTask
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    // Personal Task methods
    fun getAllPersonalTasks(): Flow<List<PersonalTask>>
    suspend fun getPersonalTask(id: String): PersonalTask?
    suspend fun createPersonalTask(task: PersonalTask): Result<PersonalTask>
    suspend fun updatePersonalTask(task: PersonalTask): Result<PersonalTask>
    suspend fun deletePersonalTask(taskId: String): Result<Unit>
    suspend fun syncPersonalTasks(): Result<Unit>

    // Team Task methods
    fun getAllTeamTasks(): Flow<List<TeamTask>>
    fun getTeamTasks(teamId: String): Flow<List<TeamTask>>
    fun getTasksAssignedToUser(userId: String): Flow<List<TeamTask>>
    suspend fun getTeamTask(id: String): TeamTask?
    suspend fun createTeamTask(task: TeamTask): Result<TeamTask>
    suspend fun updateTeamTask(task: TeamTask): Result<TeamTask>
    suspend fun deleteTeamTask(taskId: String): Result<Unit>
    suspend fun assignTeamTask(taskId: String, userId: String?): Result<TeamTask>
    suspend fun syncTeamTasks(): Result<Unit>
    suspend fun syncTeamTasksByTeam(teamId: String): Result<Unit>
} 