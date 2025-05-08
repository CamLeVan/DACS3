package com.example.taskapplication.data.repository

import android.util.Log
import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.database.dao.PersonalTaskDao
import com.example.taskapplication.data.database.entities.PersonalTaskEntity
import com.example.taskapplication.data.mapper.toApiRequest
import com.example.taskapplication.data.mapper.toDomainModel
import com.example.taskapplication.data.mapper.toEntity
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.domain.model.PersonalTask
import com.example.taskapplication.domain.repository.PersonalTaskRepository
import com.example.taskapplication.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonalTaskRepositoryImpl @Inject constructor(
    private val personalTaskDao: PersonalTaskDao,
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager,
    private val connectionChecker: ConnectionChecker,
    private val networkUtils: NetworkUtils
) : PersonalTaskRepository {

    private val TAG = "PersonalTaskRepository"

    override fun getAllTasks(): Flow<List<PersonalTask>> {
        return personalTaskDao.getAllTasks()
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                // Log error and emit empty list
                emit(emptyList())
            }
    }

    override fun getTaskById(taskId: String): Flow<PersonalTask?> {
        return personalTaskDao.getTaskById(taskId)
            .map { entity -> entity?.toDomainModel() }
            .catch { e ->
                // Log error and emit null
                emit(null)
            }
    }

    override suspend fun createTask(task: PersonalTask): Result<PersonalTask> {
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
                val response = apiService.updatePersonalTask(task.toEntity().toApiRequest())
                if (response.isSuccessful) {
                    response.body()?.let { taskResponse ->
                        val entity = taskResponse.toEntity()
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
                                val entity = taskResponse.toEntity()
                                personalTaskDao.updateTask(entity)
                            }
                        }
                    }
                    "pending_update" -> {
                        val response = apiService.updatePersonalTask(task.toApiRequest())
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

    override fun getTasks(userId: String): Flow<List<PersonalTask>> = flow {
        emit(personalTaskDao.getTasks(userId).map { it.toDomainModel() })
    }.flowOn(Dispatchers.IO)

    override fun getTasksSync(userId: String): List<PersonalTask> {
        return personalTaskDao.getTasksSync(userId).map { it.toDomainModel() }
    }

    override fun getTaskByIdSync(id: String): PersonalTask? {
        return personalTaskDao.getTaskByIdSync(id)?.toDomainModel()
    }

    override fun getTaskByServerId(serverId: String): Flow<PersonalTask?> = flow {
        emit(personalTaskDao.getTaskByServerId(serverId)?.toDomainModel())
    }.flowOn(Dispatchers.IO)

    override fun getTaskByServerIdSync(serverId: String): PersonalTask? {
        return personalTaskDao.getTaskByServerIdSync(serverId)?.toDomainModel()
    }

    override suspend fun syncTasksByUser(userId: String): Result<Unit> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                return Result.failure(Exception("No network connection"))
            }

            val pendingTasks = personalTaskDao.getPendingTasksByUserSync(userId)
            for (task in pendingTasks) {
                when (task.syncStatus) {
                    "pending" -> {
                        val response = apiService.createPersonalTask(task.toApiRequest())
                        val entity = response.toEntity()
                        entity.syncStatus = "synced"
                        personalTaskDao.updateTask(entity)
                    }
                }
            }

            val serverTasks = apiService.getPersonalTasksByUser(userId)
            for (task in serverTasks) {
                val entity = task.toEntity()
                entity.syncStatus = "synced"
                personalTaskDao.insertTask(entity)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}