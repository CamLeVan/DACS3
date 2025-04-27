package com.example.taskapplication.data.repository

import android.util.Log
import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.database.dao.PersonalTaskDao
import com.example.taskapplication.data.mapper.toApiRequest
import com.example.taskapplication.data.mapper.toDomainModel
import com.example.taskapplication.data.mapper.toEntity
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.domain.model.PersonalTask
import com.example.taskapplication.domain.repository.PersonalTaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.*
import javax.inject.Inject

class PersonalTaskRepositoryImpl @Inject constructor(
    private val personalTaskDao: PersonalTaskDao,
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager,
    private val connectionChecker: ConnectionChecker
) : PersonalTaskRepository {
    
    override fun getAllTasks(): Flow<List<PersonalTask>> {
        return personalTaskDao.getAllTasks().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override suspend fun getTask(id: String): PersonalTask? {
        return personalTaskDao.getTaskById(id)?.toDomainModel()
    }
    
    override suspend fun createTask(task: PersonalTask): Result<PersonalTask> {
        val taskEntity = task.copy(
            id = UUID.randomUUID().toString(),
            syncStatus = "pending_create",
            lastModified = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        ).toEntity()
        
        personalTaskDao.insertTask(taskEntity)
        
        // Nếu có kết nối, thử đồng bộ ngay
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTasks()
            } catch (e: Exception) {
                // Xử lý ngoại lệ nhưng không làm ảnh hưởng đến tác vụ local
                Log.e("PersonalTaskRepo", "Error syncing after create", e)
            }
        }
        
        return Result.success(taskEntity.toDomainModel())
    }
    
    override suspend fun updateTask(task: PersonalTask): Result<PersonalTask> {
        val existingTask = personalTaskDao.getTaskById(task.id)
            ?: return Result.failure(NoSuchElementException("Task not found"))
        
        val updatedEntity = task.copy(
            syncStatus = if (existingTask.serverId != null) "pending_update" else "pending_create",
            lastModified = System.currentTimeMillis(),
            serverId = existingTask.serverId,
            createdAt = existingTask.createdAt
        ).toEntity()
        
        personalTaskDao.updateTask(updatedEntity)
        
        // Nếu có kết nối, thử đồng bộ ngay
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTasks()
            } catch (e: Exception) {
                Log.e("PersonalTaskRepo", "Error syncing after update", e)
            }
        }
        
        return Result.success(updatedEntity.toDomainModel())
    }
    
    override suspend fun deleteTask(taskId: String): Result<Unit> {
        val task = personalTaskDao.getTaskById(taskId)
            ?: return Result.failure(NoSuchElementException("Task not found"))
        
        if (task.serverId == null) {
            // Nếu task chưa được đồng bộ với server, xóa luôn
            personalTaskDao.deleteLocalOnlyTask(taskId)
        } else {
            // Đánh dấu để xóa khi đồng bộ
            personalTaskDao.markTaskForDeletion(taskId, System.currentTimeMillis())
        }
        
        // Nếu có kết nối, thử đồng bộ ngay
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTasks()
            } catch (e: Exception) {
                Log.e("PersonalTaskRepo", "Error syncing after delete", e)
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
                        personalTaskDao.insertTask(
                            task.copy(
                                serverId = serverTask.id,
                                syncStatus = "synced",
                                lastModified = System.currentTimeMillis()
                            )
                        )
                    } else {
                        Log.e("PersonalTaskRepo", "Failed to create task: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e("PersonalTaskRepo", "Error creating task", e)
                    continue
                }
            }
            
            // Process updates
            for (task in tasksToUpdate) {
                if (task.serverId == null) continue
                
                try {
                    val response = apiService.updatePersonalTask(task.serverId, task.toApiRequest())
                    if (response.isSuccessful) {
                        personalTaskDao.insertTask(
                            task.copy(
                                syncStatus = "synced",
                                lastModified = System.currentTimeMillis()
                            )
                        )
                    } else {
                        Log.e("PersonalTaskRepo", "Failed to update task: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e("PersonalTaskRepo", "Error updating task", e)
                    continue
                }
            }
            
            // Process deletes
            for (task in tasksToDelete) {
                if (task.serverId == null) continue
                
                try {
                    val response = apiService.deletePersonalTask(task.serverId)
                    if (response.isSuccessful) {
                        personalTaskDao.deleteSyncedTask(task.id)
                    } else {
                        Log.e("PersonalTaskRepo", "Failed to delete task: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e("PersonalTaskRepo", "Error deleting task", e)
                    continue
                }
            }
            
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PersonalTaskRepo", "Error during sync", e)
            return Result.failure(e)
        }
    }
} 