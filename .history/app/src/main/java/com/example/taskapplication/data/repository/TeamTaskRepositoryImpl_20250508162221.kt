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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emit
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
        return teamTaskDao.getTasksByTeam(teamId)
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                // Log error and emit empty list
                emit(emptyList())
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
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.createTeamTask(task.toEntity().toApiRequest())
                if (response.isSuccessful) {
                    response.body()?.let { taskResponse ->
                        val entity = taskResponse.toEntity()
                        teamTaskDao.insertTask(entity)
                        Result.success(entity.toDomainModel())
                    } ?: Result.failure(IOException("Empty response from server"))
                } else {
                    // If server fails, save locally with pending status
                    val entity = task.toEntity().copy(syncStatus = "pending_create")
                    teamTaskDao.insertTask(entity)
                    Result.success(entity.toDomainModel())
                }
            } else {
                // Save locally with pending status
                val entity = task.toEntity().copy(syncStatus = "pending_create")
                teamTaskDao.insertTask(entity)
                Result.success(entity.toDomainModel())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTask(task: TeamTask): Result<TeamTask> {
        return try {
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.updateTeamTask(task.id, task.toEntity().toApiRequest())
                if (response.isSuccessful) {
                    response.body()?.let { taskResponse ->
                        val entity = taskResponse.toEntity()
                        teamTaskDao.updateTask(entity)
                        Result.success(entity.toDomainModel())
                    } ?: Result.failure(IOException("Empty response from server"))
                } else {
                    // If server fails, update locally with pending status
                    val entity = task.toEntity().copy(syncStatus = "pending_update")
                    teamTaskDao.updateTask(entity)
                    Result.success(entity.toDomainModel())
                }
            } else {
                // Update locally with pending status
                val entity = task.toEntity().copy(syncStatus = "pending_update")
                teamTaskDao.updateTask(entity)
                Result.success(entity.toDomainModel())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> {
        return try {
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.deleteTeamTask(taskId)
                if (response.isSuccessful) {
                    teamTaskDao.deleteTask(taskId)
                    Result.success(Unit)
                } else {
                    // If server fails, mark for deletion locally
                    teamTaskDao.markTaskForDeletion(taskId, System.currentTimeMillis())
                    Result.success(Unit)
                }
            } else {
                // Mark for deletion locally
                teamTaskDao.markTaskForDeletion(taskId, System.currentTimeMillis())
                Result.success(Unit)
            }
        } catch (e: Exception) {
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
                if (response.isSuccessful) {
                    response.body()?.let { taskResponse ->
                        val entity = taskResponse.toEntity()
                        entity.syncStatus = "synced"
                        teamTaskDao.updateTask(entity)
                        Result.success(entity.toDomainModel())
                    } ?: Result.failure(IOException("Empty response from server"))
                } else {
                    Result.failure(IOException("Failed to assign task"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error assigning task", e)
            Result.failure(e)
        }
    }

    override suspend fun syncTasks(): Result<Unit> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                return Result.failure(IOException("No network connection"))
            }

            // Get pending tasks
            val pendingTasks = teamTaskDao.getPendingSyncTasks()

            // Sync each pending task
            for (task in pendingTasks) {
                when (task.syncStatus) {
                    "pending_create" -> {
                        val response = apiService.createTeamTask(task.toApiRequest())
                        if (response.isSuccessful) {
                            response.body()?.let { taskResponse ->
                                val entity = taskResponse.toEntity()
                                teamTaskDao.updateTask(entity)
                            }
                        }
                    }
                    "pending_update" -> {
                        val response = apiService.updateTeamTask(task.id, task.toApiRequest())
                        if (response.isSuccessful) {
                            response.body()?.let { taskResponse ->
                                val entity = taskResponse.toEntity()
                                teamTaskDao.updateTask(entity)
                            }
                        }
                    }
                    "pending_delete" -> {
                        val response = apiService.deleteTeamTask(task.id)
                        if (response.isSuccessful) {
                            teamTaskDao.deleteTask(task.id)
                        }
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncTasksByTeam(teamId: String): Result<Unit> {
        if (!networkUtils.isNetworkAvailable()) {
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
                            response.body()?.let { taskResponse ->
                                val entity = taskResponse.toEntity()
                                teamTaskDao.updateTask(entity)
                            }
                        }
                    }
                    "pending_update" -> {
                        val response = apiService.updateTeamTask(task.id, task.toApiRequest())
                        if (response.isSuccessful) {
                            response.body()?.let { taskResponse ->
                                val entity = taskResponse.toEntity()
                                teamTaskDao.updateTask(entity)
                            }
                        }
                    }
                    "pending_delete" -> {
                        val response = apiService.deleteTeamTask(task.id)
                        if (response.isSuccessful) {
                            teamTaskDao.deleteTask(task.id)
                        }
                    }
                }
            }

            // Sync server tasks
            val response = apiService.getTeamTasksByTeam(teamId)
            if (response.isSuccessful) {
                response.body()?.let { serverTasks ->
                    for (serverTask in serverTasks) {
                        val existingTask = teamTaskDao.getTaskByServerId(serverTask.id.toString())
                        if (existingTask == null) {
                            teamTaskDao.insertTask(serverTask.toEntity())
                        }
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
