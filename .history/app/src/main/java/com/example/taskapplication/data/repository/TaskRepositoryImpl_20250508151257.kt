package com.example.taskapplication.data.repository

import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.database.dao.PersonalTaskDao
import com.example.taskapplication.data.database.dao.TeamTaskDao
import com.example.taskapplication.data.mapper.toApiRequest
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

    // Personal Task implementations
    override fun getAllPersonalTasks(): Flow<List<PersonalTask>> {
        return personalTaskDao.getAllTasks()
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                emit(emptyList())
            }
    }

    override suspend fun getPersonalTask(id: String): PersonalTask? {
        return personalTaskDao.getTaskById(id)?.toDomainModel()
    }

    override suspend fun createPersonalTask(task: PersonalTask): Result<PersonalTask> {
        return try {
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.createPersonalTask(task.toEntity().toApiRequest())
                if (response.isSuccessful) {
                    response.body()?.let { taskResponse ->
                        val entity = taskResponse.toEntity()
                        personalTaskDao.insertTask(entity)
                        Result.success(entity.toDomainModel())
                    } ?: Result.failure(IOException("Empty response from server"))
                } else {
                    val entity = task.toEntity().copy(syncStatus = "pending_create")
                    personalTaskDao.insertTask(entity)
                    Result.success(entity.toDomainModel())
                }
            } else {
                val entity = task.toEntity().copy(syncStatus = "pending_create")
                personalTaskDao.insertTask(entity)
                Result.success(entity.toDomainModel())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePersonalTask(task: PersonalTask): Result<PersonalTask> {
        return try {
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.updatePersonalTask(task.id, task.toEntity().toApiRequest())
                if (response.isSuccessful) {
                    response.body()?.let { taskResponse ->
                        val entity = taskResponse.toEntity()
                        personalTaskDao.updateTask(entity)
                        Result.success(entity.toDomainModel())
                    } ?: Result.failure(IOException("Empty response from server"))
                } else {
                    val entity = task.toEntity().copy(syncStatus = "pending_update")
                    personalTaskDao.updateTask(entity)
                    Result.success(entity.toDomainModel())
                }
            } else {
                val entity = task.toEntity().copy(syncStatus = "pending_update")
                personalTaskDao.updateTask(entity)
                Result.success(entity.toDomainModel())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePersonalTask(taskId: String): Result<Unit> {
        return try {
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.deletePersonalTask(taskId)
                if (response.isSuccessful) {
                    personalTaskDao.deleteTask(taskId)
                    Result.success(Unit)
                } else {
                    personalTaskDao.markTaskForDeletion(taskId, System.currentTimeMillis())
                    Result.success(Unit)
                }
            } else {
                personalTaskDao.markTaskForDeletion(taskId, System.currentTimeMillis())
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncPersonalTasks(): Result<Unit> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                return Result.failure(IOException("No network connection"))
            }

            val pendingTasks = personalTaskDao.getPendingSyncTasks()
            for (task in pendingTasks) {
                when (task.syncStatus) {
                    "pending_create" -> {
                        val response = apiService.createPersonalTask(task.toApiRequest())
                        if (response.isSuccessful) {
                            response.body()?.let { taskResponse ->
                                val entity = taskResponse.toEntity()
                                personalTaskDao.updateTask(entity)
                            }
                        }
                    }
                    "pending_update" -> {
                        val response = apiService.updatePersonalTask(task.id, task.toApiRequest())
                        if (response.isSuccessful) {
                            response.body()?.let { taskResponse ->
                                val entity = taskResponse.toEntity()
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

    // Team Task implementations
    override fun getAllTeamTasks(): Flow<List<TeamTask>> {
        return teamTaskDao.getAllTeamTasks()
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                emit(emptyList())
            }
    }

    override fun getTeamTasks(teamId: String): Flow<List<TeamTask>> {
        return teamTaskDao.getTasksByTeam(teamId)
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                emit(emptyList())
            }
    }

    override fun getTasksAssignedToUser(userId: String): Flow<List<TeamTask>> {
        return teamTaskDao.getTasksAssignedToUser(userId)
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                emit(emptyList())
            }
    }

    override suspend fun getTeamTask(id: String): TeamTask? {
        return teamTaskDao.getTaskById(id)?.toDomainModel()
    }

    override suspend fun createTeamTask(task: TeamTask): Result<TeamTask> {
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
                    val entity = task.toEntity().copy(syncStatus = "pending_create")
                    teamTaskDao.insertTask(entity)
                    Result.success(entity.toDomainModel())
                }
            } else {
                val entity = task.toEntity().copy(syncStatus = "pending_create")
                teamTaskDao.insertTask(entity)
                Result.success(entity.toDomainModel())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTeamTask(task: TeamTask): Result<TeamTask> {
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
                    val entity = task.toEntity().copy(syncStatus = "pending_update")
                    teamTaskDao.updateTask(entity)
                    Result.success(entity.toDomainModel())
                }
            } else {
                val entity = task.toEntity().copy(syncStatus = "pending_update")
                teamTaskDao.updateTask(entity)
                Result.success(entity.toDomainModel())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTeamTask(taskId: String): Result<Unit> {
        return try {
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.deleteTeamTask(taskId)
                if (response.isSuccessful) {
                    teamTaskDao.deleteTask(taskId)
                    Result.success(Unit)
                } else {
                    teamTaskDao.markTaskForDeletion(taskId, System.currentTimeMillis())
                    Result.success(Unit)
                }
            } else {
                teamTaskDao.markTaskForDeletion(taskId, System.currentTimeMillis())
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun assignTeamTask(taskId: String, userId: String?): Result<TeamTask> {
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
            Result.failure(e)
        }
    }

    override suspend fun syncTeamTasks(): Result<Unit> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                return Result.failure(IOException("No network connection"))
            }

            val pendingTasks = teamTaskDao.getPendingSyncTasks()
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

    override suspend fun syncTeamTasksByTeam(teamId: String): Result<Unit> {
        if (!networkUtils.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        return try {
            val pendingTasks = teamTaskDao.getPendingSyncTasksByTeam(teamId)
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