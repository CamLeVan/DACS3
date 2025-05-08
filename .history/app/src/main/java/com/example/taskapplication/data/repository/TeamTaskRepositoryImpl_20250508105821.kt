package com.example.taskapplication.data.repository

import android.util.Log
import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.database.dao.TeamTaskDao
import com.example.taskapplication.data.mapper.toApiRequest
import com.example.taskapplication.data.mapper.toDomainModel
import com.example.taskapplication.data.mapper.toEntity
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.domain.model.TeamTask
import com.example.taskapplication.domain.repository.TeamTaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeamTaskRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val teamTaskDao: TeamTaskDao,
    private val connectionChecker: ConnectionChecker,
    private val dataStoreManager: DataStoreManager
) : TeamTaskRepository {

    private val TAG = "TeamTaskRepository"

    override fun getAllTeamTasks(): Flow<List<TeamTask>> {
        return teamTaskDao.getAllTasks().map { tasks ->
            tasks.map { it.toDomainModel() }
        }
    }

    override fun getTasksForTeam(teamId: String): Flow<List<TeamTask>> {
        return teamTaskDao.getTasksForTeam(teamId).map { tasks ->
            tasks.map { it.toDomainModel() }
        }
    }

    override fun getTask(taskId: String): Flow<TeamTask?> {
        return teamTaskDao.getTask(taskId).map { it?.toDomainModel() }
    }

    override suspend fun createTask(task: TeamTask): Result<TeamTask> {
        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        val newTask = task.copy(
            id = UUID.randomUUID().toString(),
            syncStatus = "pending",
            createdBy = currentUserId
        )

        return try {
            teamTaskDao.insertTask(newTask.toEntity())
            Result.success(newTask)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating task", e)
            Result.failure(e)
        }
    }

    override suspend fun updateTask(task: TeamTask): Result<TeamTask> {
        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        val existingTask = teamTaskDao.getTaskSync(task.id) ?:
            return Result.failure(IllegalStateException("Task not found"))

        if (existingTask.createdBy != currentUserId) {
            return Result.failure(IllegalStateException("Not authorized to update this task"))
        }

        val updatedTask = task.copy(
            syncStatus = "pending",
            lastModified = System.currentTimeMillis()
        )

        return try {
            teamTaskDao.updateTask(updatedTask.toEntity())
            Result.success(updatedTask)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating task", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> {
        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        val existingTask = teamTaskDao.getTaskSync(taskId) ?:
            return Result.failure(IllegalStateException("Task not found"))

        if (existingTask.createdBy != currentUserId) {
            return Result.failure(IllegalStateException("Not authorized to delete this task"))
        }

        return try {
            teamTaskDao.deleteTask(taskId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting task", e)
            Result.failure(e)
        }
    }

    override suspend fun syncTasks(): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        return try {
            // Sync pending tasks
            val pendingTasks = teamTaskDao.getPendingTasks()
            for (task in pendingTasks) {
                when (task.syncStatus) {
                    "pending_create" -> {
                        val response = apiService.createTeamTask(task.teamId, task.toApiRequest())
                        if (response.isSuccessful) {
                            val serverId = response.body()?.id.toString()
                            teamTaskDao.updateTaskServerId(task.id, serverId)
                            teamTaskDao.markTaskAsSynced(task.id)
                        }
                    }
                    "pending_update" -> {
                        val response = apiService.updateTeamTask(task.teamId, task.serverId!!, task.toApiRequest())
                        if (response.isSuccessful) {
                            teamTaskDao.markTaskAsSynced(task.id)
                        }
                    }
                    "pending_delete" -> {
                        val response = apiService.deleteTeamTask(task.teamId, task.serverId!!)
                        if (response.isSuccessful) {
                            teamTaskDao.deleteTask(task.id)
                        }
                    }
                }
            }

            // Sync server tasks
            val response = apiService.getTeamTasks()
            if (response.isSuccessful) {
                val serverTasks = response.body() ?: emptyList()
                for (serverTask in serverTasks) {
                    val existingTask = teamTaskDao.getTaskByServerId(serverTask.id.toString())
                    if (existingTask == null) {
                        teamTaskDao.insertTask(serverTask.toEntity())
                    } else {
                        teamTaskDao.updateTask(serverTask.toEntity(existingTask))
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing tasks", e)
            Result.failure(e)
        }
    }

    override suspend fun syncTasksByTeam(teamId: String): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        return try {
            // Sync pending tasks for this team
            val pendingTasks = teamTaskDao.getPendingTasksForTeam(teamId)
            for (task in pendingTasks) {
                when (task.syncStatus) {
                    "pending_create" -> {
                        val response = apiService.createTeamTask(task.teamId, task.toApiRequest())
                        if (response.isSuccessful) {
                            val serverId = response.body()?.id.toString()
                            teamTaskDao.updateTaskServerId(task.id, serverId)
                            teamTaskDao.markTaskAsSynced(task.id)
                        }
                    }
                    "pending_update" -> {
                        val response = apiService.updateTeamTask(task.teamId, task.serverId!!, task.toApiRequest())
                        if (response.isSuccessful) {
                            teamTaskDao.markTaskAsSynced(task.id)
                        }
                    }
                    "pending_delete" -> {
                        val response = apiService.deleteTeamTask(task.teamId, task.serverId!!)
                        if (response.isSuccessful) {
                            teamTaskDao.deleteTask(task.id)
                        }
                    }
                }
            }

            // Sync server tasks for this team
            val response = apiService.getTeamTasksByTeam(teamId)
            if (response.isSuccessful) {
                val serverTasks = response.body() ?: emptyList()
                for (serverTask in serverTasks) {
                    val existingTask = teamTaskDao.getTaskByServerId(serverTask.id.toString())
                    if (existingTask == null) {
                        teamTaskDao.insertTask(serverTask.toEntity())
                    } else {
                        teamTaskDao.updateTask(serverTask.toEntity(existingTask))
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing tasks for team $teamId", e)
            Result.failure(e)
        }
    }
}
