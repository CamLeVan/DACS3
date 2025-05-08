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

    override fun getTasks(): Flow<List<TeamTask>> = flow {
        val tasks = teamTaskDao.getAllTasks().map { it.toDomainModel() }
        emit(tasks)
    }

    override fun getTasksByTeam(teamId: String): Flow<List<TeamTask>> = flow {
        val tasks = teamTaskDao.getTasksByTeam(teamId.toLong()).map { it.toDomainModel() }
        emit(tasks)
    }

    override suspend fun getTasksSync(): List<TeamTask> = withContext(Dispatchers.IO) {
        teamTaskDao.getAllTasksSync().map { it.toDomainModel() }
    }

    override suspend fun getTaskById(id: String): TeamTask? = withContext(Dispatchers.IO) {
        teamTaskDao.getTaskById(id.toLong())?.toDomainModel()
    }

    override suspend fun getTaskByIdSync(id: String): TeamTask? = withContext(Dispatchers.IO) {
        teamTaskDao.getTaskByIdSync(id.toLong())?.toDomainModel()
    }

    override suspend fun getTaskByServerId(serverId: String): TeamTask? = withContext(Dispatchers.IO) {
        teamTaskDao.getTaskByServerId(serverId)?.toDomainModel()
    }

    override suspend fun getTaskByServerIdSync(serverId: String): TeamTask? = withContext(Dispatchers.IO) {
        teamTaskDao.getTaskByServerIdSync(serverId)?.toDomainModel()
    }

    override suspend fun createTask(task: TeamTask): Result<TeamTask> = withContext(Dispatchers.IO) {
        try {
            val entity = task.toEntity()
            entity.syncStatus = if (networkUtils.isNetworkAvailable()) "synced" else "pending"
            val id = teamTaskDao.insertTask(entity)
            val createdTask = teamTaskDao.getTaskById(id)?.toDomainModel()
                ?: return@withContext Result.failure(Exception("Failed to create task"))
            
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.createTeamTask(entity.toApiRequest())
                val updatedEntity = response.toEntity(entity)
                teamTaskDao.updateTask(updatedEntity)
                Result.success(updatedEntity.toDomainModel())
            } else {
                Result.success(createdTask)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTask(task: TeamTask): Result<TeamTask> = withContext(Dispatchers.IO) {
        try {
            val entity = task.toEntity()
            entity.syncStatus = if (networkUtils.isNetworkAvailable()) "synced" else "pending"
            teamTaskDao.updateTask(entity)
            val updatedTask = teamTaskDao.getTaskById(entity.id)?.toDomainModel()
                ?: return@withContext Result.failure(Exception("Failed to update task"))
            
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.updateTeamTask(entity.id, entity.toApiRequest())
                val updatedEntity = response.toEntity(entity)
                teamTaskDao.updateTask(updatedEntity)
                Result.success(updatedEntity.toDomainModel())
            } else {
                Result.success(updatedTask)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTask(task: TeamTask): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = task.toEntity()
            if (networkUtils.isNetworkAvailable()) {
                apiService.deleteTeamTask(entity.id)
                teamTaskDao.deleteTask(entity)
                Result.success(Unit)
            } else {
                entity.syncStatus = "pending"
                teamTaskDao.updateTask(entity)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncTasks(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!networkUtils.isNetworkAvailable()) {
                return@withContext Result.failure(Exception("No network connection"))
            }

            val pendingTasks = teamTaskDao.getPendingTasksSync()
            for (task in pendingTasks) {
                when (task.syncStatus) {
                    "pending" -> {
                        val response = apiService.createTeamTask(task.toApiRequest())
                        val updatedEntity = response.toEntity(task)
                        teamTaskDao.updateTask(updatedEntity)
                    }
                    "deleted" -> {
                        apiService.deleteTeamTask(task.id)
                        teamTaskDao.deleteTask(task)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncTasksByTeam(teamId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!networkUtils.isNetworkAvailable()) {
                return@withContext Result.failure(Exception("No network connection"))
            }

            val pendingTasks = teamTaskDao.getPendingTasksByTeamSync(teamId.toLong())
            for (task in pendingTasks) {
                when (task.syncStatus) {
                    "pending" -> {
                        val response = apiService.createTeamTask(task.toApiRequest())
                        val updatedEntity = response.toEntity(task)
                        teamTaskDao.updateTask(updatedEntity)
                    }
                    "deleted" -> {
                        apiService.deleteTeamTask(task.id)
                        teamTaskDao.deleteTask(task)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
