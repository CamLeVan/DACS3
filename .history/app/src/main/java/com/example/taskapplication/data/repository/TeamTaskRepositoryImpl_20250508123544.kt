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
import com.example.taskapplication.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeamTaskRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val teamTaskDao: TeamTaskDao,
    private val connectionChecker: ConnectionChecker,
    private val dataStoreManager: DataStoreManager,
    private val networkUtils: NetworkUtils
) : TeamTaskRepository {

    private val TAG = "TeamTaskRepository"

    override fun getAllTeamTasks(): Flow<List<TeamTask>> {
        return teamTaskDao.getAllTasks().map { tasks ->
            tasks.map { it.toDomainModel() }
        }
    }

    override fun getTasksByTeam(teamId: String): Flow<List<TeamTask>> {
        return teamTaskDao.getTasksByTeam(teamId).map { tasks ->
            tasks.map { it.toDomainModel() }
        }
    }

    override fun getTasksAssignedToUser(userId: String): Flow<List<TeamTask>> {
        return teamTaskDao.getTasksAssignedToUser(userId).map { tasks ->
            tasks.map { it.toDomainModel() }
        }
    }

    override suspend fun getTaskById(id: String): TeamTask? {
        return teamTaskDao.getTaskById(id)?.toDomainModel()
    }

    override suspend fun createTask(task: TeamTask): Result<TeamTask> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                val entity = task.toEntity()
                entity.syncStatus = "pending"
                teamTaskDao.insertTask(entity)
                Result.success(entity.toDomainModel())
            } else {
                val response = apiService.createTeamTask(task.toApiRequest())
                val entity = response.toEntity()
                entity.syncStatus = "synced"
                teamTaskDao.insertTask(entity)
                Result.success(entity.toDomainModel())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating task", e)
            Result.failure(e)
        }
    }

    override suspend fun updateTask(task: TeamTask): Result<TeamTask> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                val entity = task.toEntity()
                entity.syncStatus = "pending"
                teamTaskDao.updateTask(entity)
                Result.success(entity.toDomainModel())
            } else {
                val response = apiService.updateTeamTask(task.id, task.toApiRequest())
                val entity = response.toEntity()
                entity.syncStatus = "synced"
                teamTaskDao.updateTask(entity)
                Result.success(entity.toDomainModel())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating task", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                val task = teamTaskDao.getTaskById(taskId)
                if (task != null) {
                    task.syncStatus = "pending"
                    teamTaskDao.updateTask(task)
                }
                Result.success(Unit)
            } else {
                apiService.deleteTeamTask(taskId)
                teamTaskDao.deleteTask(taskId)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting task", e)
            Result.failure(e)
        }
    }

    override suspend fun assignTask(taskId: String, userId: String?): Result<TeamTask> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                val task = teamTaskDao.getTaskById(taskId)
                    ?: return Result.failure(IllegalStateException("Task not found"))
                
                task.assignedTo = userId
                task.syncStatus = "pending"
                teamTaskDao.updateTask(task)
                Result.success(task.toDomainModel())
            } else {
                val response = apiService.assignTeamTask(taskId, userId)
                val entity = response.toEntity()
                entity.syncStatus = "synced"
                teamTaskDao.updateTask(entity)
                Result.success(entity.toDomainModel())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error assigning task", e)
            Result.failure(e)
        }
    }

    override suspend fun syncTasks(): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        return try {
            // Sync pending tasks
            val pendingTasks = teamTaskDao.getPendingTasksSync()
            for (task in pendingTasks) {
                when (task.syncStatus) {
                    "pending_create" -> {
                        val response = apiService.createTeamTask(task.toApiRequest())
                        if (response.isSuccessful) {
                            val serverId = response.body()?.id.toString()
                            teamTaskDao.updateTaskServerId(task.id, serverId)
                            teamTaskDao.markTaskAsSynced(task.id)
                        }
                    }
                    "pending_update" -> {
                        val response = apiService.updateTeamTask(task.serverId!!, task.toApiRequest())
                        if (response.isSuccessful) {
                            teamTaskDao.markTaskAsSynced(task.id)
                        }
                    }
                    "pending_delete" -> {
                        val response = apiService.deleteTeamTask(task.serverId!!)
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

        return try {
            // Sync pending tasks
            val pendingTasks = teamTaskDao.getPendingTasksByTeamSync(teamId)
            for (task in pendingTasks) {
                when (task.syncStatus) {
                    "pending_create" -> {
                        val response = apiService.createTeamTask(task.toApiRequest())
                        if (response.isSuccessful) {
                            val serverId = response.body()?.id.toString()
                            teamTaskDao.updateTaskServerId(task.id, serverId)
                            teamTaskDao.markTaskAsSynced(task.id)
                        }
                    }
                    "pending_update" -> {
                        val response = apiService.updateTeamTask(task.serverId!!, task.toApiRequest())
                        if (response.isSuccessful) {
                            teamTaskDao.markTaskAsSynced(task.id)
                        }
                    }
                    "pending_delete" -> {
                        val response = apiService.deleteTeamTask(task.serverId!!)
                        if (response.isSuccessful) {
                            teamTaskDao.deleteTask(task.id)
                        }
                    }
                }
            }

            // Sync server tasks
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
            Log.e(TAG, "Error syncing tasks by team", e)
            Result.failure(e)
        }
    }
}
