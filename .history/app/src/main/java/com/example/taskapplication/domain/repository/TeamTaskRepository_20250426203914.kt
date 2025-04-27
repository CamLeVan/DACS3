package com.example.taskapplication.domain.repository

import com.example.taskapplication.domain.model.TeamTask
import kotlinx.coroutines.flow.Flow

interface TeamTaskRepository {
    fun getAllTeamTasks(): Flow<List<TeamTask>>
    fun getTasksByTeam(teamId: String): Flow<List<TeamTask>>
    fun getTasksAssignedToUser(userId: String): Flow<List<TeamTask>>
    suspend fun getTaskById(id: String): TeamTask?
    suspend fun createTask(task: TeamTask): Result<TeamTask>
    suspend fun updateTask(task: TeamTask): Result<TeamTask>
    suspend fun deleteTask(taskId: String): Result<Unit>
    suspend fun assignTask(taskId: String, userId: String?): Result<TeamTask>
    suspend fun syncTasks(): Result<Unit>
    suspend fun syncTasksByTeam(teamId: String): Result<Unit>
} 