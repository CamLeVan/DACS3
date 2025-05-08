package com.example.taskapplication.data.repository

import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.database.dao.PersonalTaskDao
import com.example.taskapplication.data.database.dao.TeamTaskDao
import com.example.taskapplication.data.mapper.toDomainModel
import com.example.taskapplication.data.mapper.toEntity
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.domain.model.PersonalTask
import com.example.taskapplication.domain.model.TeamTask
import com.example.taskapplication.domain.repository.TaskRepository
import com.example.taskapplication.util.NetworkUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val personalTaskDao: PersonalTaskDao,
    private val teamTaskDao: TeamTaskDao,
    private val apiService: ApiService,
    private val connectionChecker: ConnectionChecker,
    private val networkUtils: NetworkUtils
) : TaskRepository {

    override fun getAllTasks(): Flow<List<PersonalTask>> {
        return personalTaskDao.getAllTasks()
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                // Log error and emit empty list
                emit(emptyList())
            }
    }

    override fun getTeamTasks(teamId: String): Flow<List<TeamTask>> {
        return teamTaskDao.getTasksByTeam(teamId)
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                // Log error and emit empty list
                emit(emptyList())
            }
    }

    override suspend fun createTask(task: PersonalTask): Result<PersonalTask> {
        return try {
            if (networkUtils.isNetworkAvailable()) {
                // Try to create on server first
                val response = apiService.createPersonalTask(task.toEntity().toApiRequest())
                if (response.isSuccessful) {
                    response.body()?.let { taskResponse ->
                        val entity = taskResponse.toEntity(task)
                        personalTaskDao.insertTask(entity)
                        Result.success(entity.toDomainModel())
                    } ?: Result.failure(IOException("Empty response from server"))
                } else {
                    // If server fails, save locally with pending status
                    val entity = task.toEntity().copy(syncStatus = "pending_create")
                    personalTaskDao.insertTask(entity)
                    Result.success(entity.toDomainModel())
                }
            } else {
                // Save locally with pending status
                val entity = task.toEntity().copy(syncStatus = "pending_create")
                personalTaskDao.insertTask(entity)
                Result.success(entity.toDomainModel())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTask(task: PersonalTask): Result<PersonalTask> {
        return try {
            if (networkUtils.isNetworkAvailable()) {
                // Try to update on server first
                val response = apiService.updatePersonalTask(task.toEntity().toApiRequest())
                if (response.isSuccessful) {
                    response.body()?.let { taskResponse ->
                        val entity = taskResponse.toEntity(task)
                        personalTaskDao.updateTask(entity)
                        Result.success(entity.toDomainModel())
                    } ?: Result.failure(IOException("Empty response from server"))
                } else {
                    // If server fails, update locally with pending status
                    val entity = task.toEntity().copy(syncStatus = "pending_update")
                    personalTaskDao.updateTask(entity)
                    Result.success(entity.toDomainModel())
                }
            } else {
                // Update locally with pending status
                val entity = task.toEntity().copy(syncStatus = "pending_update")
                personalTaskDao.updateTask(entity)
                Result.success(entity.toDomainModel())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> {
        return try {
            if (networkUtils.isNetworkAvailable()) {
                // Try to delete on server first
                val response = apiService.deletePersonalTask(taskId)
                if (response.isSuccessful) {
                    personalTaskDao.deleteTask(taskId)
                    Result.success(Unit)
                } else {
                    // If server fails, mark for deletion locally
                    personalTaskDao.markTaskForDeletion(taskId, System.currentTimeMillis())
                    Result.success(Unit)
                }
            } else {
                // Mark for deletion locally
                personalTaskDao.markTaskForDeletion(taskId, System.currentTimeMillis())
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncTasks(): Result<Unit> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                return Result.failure(IOException("No network connection"))
            }

            // Get pending tasks
            val pendingTasks = personalTaskDao.getPendingSyncTasks()

            // Sync each pending task
            for (task in pendingTasks) {
                when (task.syncStatus) {
                    "pending_create" -> {
                        val response = apiService.createPersonalTask(task.toApiRequest())
                        if (response.isSuccessful) {
                            response.body()?.let { taskResponse ->
                                val entity = taskResponse.toEntity(task)
                                personalTaskDao.updateTask(entity)
                            }
                        }
                    }
                    "pending_update" -> {
                        val response = apiService.updatePersonalTask(task.toApiRequest())
                        if (response.isSuccessful) {
                            response.body()?.let { taskResponse ->
                                val entity = taskResponse.toEntity(task)
                                personalTaskDao.updateTask(entity)
                            }
                        }
                    }
                    "pending_delete" -> {
                        val response = apiService.deletePersonalTask(task.id)
                        if (response.isSuccessful) {
                            personalTaskDao.deleteTask(task.id)
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