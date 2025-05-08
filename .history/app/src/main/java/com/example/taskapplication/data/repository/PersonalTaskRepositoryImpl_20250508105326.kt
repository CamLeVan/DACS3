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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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
        return personalTaskDao.getAllTasks().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getTask(id: String): PersonalTask? {
        return personalTaskDao.getTaskById(id)?.toDomainModel()
    }

    override suspend fun createTask(task: PersonalTask): Result<PersonalTask> {
        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        val taskWithId = task.copy(
            id = UUID.randomUUID().toString(),
            syncStatus = "pending_create",
            lastModified = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )

        personalTaskDao.insertTask(taskWithId.toEntity())

        // Nếu có kết nối, thử đồng bộ ngay
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTasks()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after creating task", e)
            }
        }

        return Result.success(taskWithId)
    }

    override suspend fun updateTask(task: PersonalTask): Result<PersonalTask> {
        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        val existingTask = personalTaskDao.getTaskById(task.id)
            ?: return Result.failure(NoSuchElementException("Task not found"))

        val updatedTask = task.copy(
            syncStatus = if (existingTask.serverId != null) "pending_update" else "pending_create",
            lastModified = System.currentTimeMillis(),
            serverId = existingTask.serverId,
            createdAt = existingTask.createdAt
        )

        personalTaskDao.updateTask(updatedTask.toEntity())

        // Nếu có kết nối, thử đồng bộ ngay
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTasks()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after updating task", e)
            }
        }

        return Result.success(updatedTask)
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> {
        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        val task = personalTaskDao.getTaskById(taskId)
            ?: return Result.failure(NoSuchElementException("Task not found"))

        if (task.serverId == null) {
            // Nếu task chưa được đồng bộ với server, xóa luôn
            personalTaskDao.deleteTask(taskId)
        } else {
            // Đánh dấu để xóa khi đồng bộ
            personalTaskDao.markTaskForDeletion(taskId, System.currentTimeMillis())
        }

        // Nếu có kết nối, thử đồng bộ ngay
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTasks()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after deleting task", e)
            }
        }

        return Result.success(Unit)
    }

    override suspend fun syncTasks(): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        try {
            val pendingTasks = personalTaskDao.getPendingSyncTasks()

            // Group by sync status
            val tasksToCreate = pendingTasks.filter { it.syncStatus == "pending_create" }
            val tasksToUpdate = pendingTasks.filter { it.syncStatus == "pending_update" }
            val tasksToDelete = pendingTasks.filter { it.syncStatus == "pending_delete" }

            // Process creates
            for (task in tasksToCreate) {
                try {
                    val response = apiService.createPersonalTask(task.toApiRequest())
                    if (response.isSuccessful && response.body() != null) {
                        val serverTask = response.body()!!
                        personalTaskDao.updateTaskServerId(task.id, serverTask.id)
                        personalTaskDao.markTaskAsSynced(task.id)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating task on server", e)
                }
            }

            // Process updates
            for (task in tasksToUpdate) {
                try {
                    val response = apiService.updatePersonalTask(task.serverId!!, task.toApiRequest())
                    if (response.isSuccessful) {
                        personalTaskDao.markTaskAsSynced(task.id)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating task on server", e)
                }
            }

            // Process deletes
            for (task in tasksToDelete) {
                try {
                    val response = apiService.deletePersonalTask(task.serverId!!)
                    if (response.isSuccessful) {
                        personalTaskDao.deleteTask(task.id)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting task on server", e)
                }
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing tasks", e)
            return Result.failure(e)
        }
    }

    override fun getTasks(userId: String): Flow<List<PersonalTask>> = flow {
        emit(personalTaskDao.getTasks(userId).map { it.toDomainModel() })
    }.flowOn(Dispatchers.IO)

    override fun getTasksSync(userId: String): List<PersonalTask> {
        return personalTaskDao.getTasksSync(userId).map { it.toDomainModel() }
    }

    override fun getTaskById(id: String): Flow<PersonalTask?> = flow {
        emit(personalTaskDao.getTaskById(id)?.toDomainModel())
    }.flowOn(Dispatchers.IO)

    override fun getTaskByIdSync(id: String): PersonalTask? {
        return personalTaskDao.getTaskByIdSync(id)?.toDomainModel()
    }

    override fun getTaskByServerId(serverId: String): Flow<PersonalTask?> = flow {
        emit(personalTaskDao.getTaskByServerId(serverId)?.toDomainModel())
    }.flowOn(Dispatchers.IO)

    override fun getTaskByServerIdSync(serverId: String): PersonalTask? {
        return personalTaskDao.getTaskByServerIdSync(serverId)?.toDomainModel()
    }

    override suspend fun syncTasksByUser(userId: String) {
        withContext(Dispatchers.IO) {
            if (!networkUtils.isNetworkAvailable()) return@withContext

            // Sync pending tasks for user
            val pendingTasks = personalTaskDao.getPendingTasksByUserSync(userId)
            for (task in pendingTasks) {
                try {
                    when (task.syncStatus) {
                        "pending" -> {
                            val response = apiService.createPersonalTask(task.toApiRequest())
                            val updatedTask = task.copy(
                                serverId = response.id.toString(),
                                syncStatus = "synced"
                            )
                            personalTaskDao.updateTask(updatedTask)
                        }
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }

            // Sync from server for user
            try {
                val serverTasks = apiService.getPersonalTasksByUser(userId)
                for (serverTask in serverTasks) {
                    val existingTask = personalTaskDao.getTaskByServerIdSync(serverTask.id.toString())
                    if (existingTask == null) {
                        personalTaskDao.insertTask(serverTask.toEntity())
                    } else if (serverTask.updatedAt > existingTask.lastModified) {
                        personalTaskDao.updateTask(serverTask.toEntity(existingTask))
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}