package com.example.taskapplication.data.repository

import android.util.Log
import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.database.dao.TeamTaskDao
import com.example.taskapplication.data.database.dao.TeamTaskAssigneeDao
import com.example.taskapplication.data.mapper.toApiRequest
import com.example.taskapplication.data.mapper.toDomainModel
import com.example.taskapplication.data.mapper.toEntity
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.domain.model.TeamTask
import com.example.taskapplication.domain.model.TeamTaskAssignee
import com.example.taskapplication.domain.repository.TeamTaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeamTaskRepositoryImpl @Inject constructor(
    private val teamTaskDao: TeamTaskDao,
    private val teamTaskAssigneeDao: TeamTaskAssigneeDao,
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager,
    private val connectionChecker: ConnectionChecker
) : TeamTaskRepository {

    private val TAG = "TeamTaskRepository"

    override fun getTeamTasks(teamId: String): Flow<List<TeamTask>> {
        return teamTaskDao.getTasksByTeamId(teamId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getTeamTaskById(taskId: String): Flow<TeamTask?> {
        return teamTaskDao.getTaskById(taskId).map { entity ->
            entity?.toDomainModel()
        }
    }

    override fun getTaskAssignees(taskId: String): Flow<List<TeamTaskAssignee>> {
        return teamTaskAssigneeDao.getAssigneesByTaskId(taskId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun createTeamTask(task: TeamTask): Result<TeamTask> {
        val taskWithId = task.copy(
            id = UUID.randomUUID().toString(),
            syncStatus = "pending_create",
            lastModified = System.currentTimeMillis()
        )
        
        teamTaskDao.insertTask(taskWithId.toEntity())
        
        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTeamTasks()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after task creation", e)
            }
        }
        
        return Result.success(taskWithId)
    }

    override suspend fun updateTeamTask(task: TeamTask): Result<TeamTask> {
        val existing = teamTaskDao.getTaskByIdSync(task.id)
            ?: return Result.failure(NoSuchElementException("Task not found"))
        
        val updatedTask = task.copy(
            syncStatus = if (existing.serverId != null) "pending_update" else "pending_create",
            lastModified = System.currentTimeMillis()
        )
        
        teamTaskDao.updateTask(updatedTask.toEntity())
        
        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTeamTasks()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after task update", e)
            }
        }
        
        return Result.success(updatedTask)
    }

    override suspend fun deleteTeamTask(taskId: String): Result<Unit> {
        val task = teamTaskDao.getTaskByIdSync(taskId)
            ?: return Result.failure(NoSuchElementException("Task not found"))
        
        if (task.serverId == null) {
            // If task has never been synced, just delete locally
            teamTaskDao.deleteTask(taskId)
            teamTaskAssigneeDao.deleteAssigneesByTaskId(taskId)
        } else {
            // Mark for deletion during next sync
            teamTaskDao.markTaskForDeletion(taskId, System.currentTimeMillis())
        }
        
        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTeamTasks()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after task deletion", e)
            }
        }
        
        return Result.success(Unit)
    }

    override suspend fun assignUserToTask(taskId: String, userId: String): Result<TeamTaskAssignee> {
        val task = teamTaskDao.getTaskByIdSync(taskId)
            ?: return Result.failure(NoSuchElementException("Task not found"))
        
        // Check if user is already assigned
        val existingAssignee = teamTaskAssigneeDao.getAssigneeSync(taskId, userId)
        if (existingAssignee != null) {
            return Result.failure(IllegalStateException("User is already assigned to this task"))
        }
        
        val assignee = TeamTaskAssignee(
            id = UUID.randomUUID().toString(),
            taskId = taskId,
            userId = userId,
            syncStatus = "pending_create",
            lastModified = System.currentTimeMillis()
        )
        
        teamTaskAssigneeDao.insertAssignee(assignee.toEntity())
        
        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTaskAssignees()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after assigning user to task", e)
            }
        }
        
        return Result.success(assignee)
    }

    override suspend fun removeUserFromTask(taskId: String, userId: String): Result<Unit> {
        val assignee = teamTaskAssigneeDao.getAssigneeSync(taskId, userId)
            ?: return Result.failure(NoSuchElementException("Task assignee not found"))
        
        if (assignee.serverId == null) {
            // If assignee has never been synced, just delete locally
            teamTaskAssigneeDao.deleteAssignee(taskId, userId)
        } else {
            // Mark for deletion during next sync
            teamTaskAssigneeDao.markAssigneeForDeletion(taskId, userId, System.currentTimeMillis())
        }
        
        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTaskAssignees()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after removing user from task", e)
            }
        }
        
        return Result.success(Unit)
    }

    override suspend fun syncTeamTasks(): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }
        
        try {
            val pendingTasks = teamTaskDao.getPendingSyncTasks()
            
            // Group by sync status
            val tasksToCreate = pendingTasks.filter { it.syncStatus == "pending_create" }
            val tasksToUpdate = pendingTasks.filter { it.syncStatus == "pending_update" }
            val tasksToDelete = pendingTasks.filter { it.syncStatus == "pending_delete" }
            
            // Process creates
            for (task in tasksToCreate) {
                try {
                    val response = apiService.createTeamTask(task.toApiRequest())
                    
                    if (response.isSuccessful && response.body() != null) {
                        val serverTask = response.body()!!
                        teamTaskDao.updateTask(
                            task.copy(
                                serverId = serverTask.id,
                                syncStatus = "synced",
                                lastModified = System.currentTimeMillis()
                            )
                        )
                    } else {
                        Log.e(TAG, "Failed to create task: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating task", e)
                    continue
                }
            }
            
            // Process updates
            for (task in tasksToUpdate) {
                if (task.serverId == null) continue
                
                try {
                    val response = apiService.updateTeamTask(task.serverId, task.toApiRequest())
                    
                    if (response.isSuccessful && response.body() != null) {
                        teamTaskDao.updateTask(
                            task.copy(
                                syncStatus = "synced",
                                lastModified = System.currentTimeMillis()
                            )
                        )
                    } else {
                        Log.e(TAG, "Failed to update task: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating task", e)
                    continue
                }
            }
            
            // Process deletes
            for (task in tasksToDelete) {
                if (task.serverId == null) {
                    // If task was never synced, we can just delete it locally
                    teamTaskDao.deleteTask(task.id)
                    continue
                }
                
                try {
                    val response = apiService.deleteTeamTask(task.serverId)
                    
                    if (response.isSuccessful) {
                        teamTaskDao.deleteTask(task.id)
                        // Also delete all assignees
                        teamTaskAssigneeDao.deleteAssigneesByTaskId(task.id)
                    } else {
                        Log.e(TAG, "Failed to delete task: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting task", e)
                    continue
                }
            }
            
            // Fetch and merge remote tasks
            try {
                val lastSyncTimestamp = dataStoreManager.getLastTeamTaskSyncTimestamp() ?: 0
                val response = apiService.getTeamTasks(lastSyncTimestamp)
                
                if (response.isSuccessful && response.body() != null) {
                    val remoteTasks = response.body()!!
                    
                    for (remoteTask in remoteTasks) {
                        val localTask = teamTaskDao.getTaskByServerIdSync(remoteTask.id)
                        
                        if (localTask == null) {
                            // New task from server
                            teamTaskDao.insertTask(
                                remoteTask.toEntity().copy(
                                    id = UUID.randomUUID().toString(),
                                    syncStatus = "synced",
                                    serverId = remoteTask.id
                                )
                            )
                        } else if (remoteTask.lastModified > localTask.lastModified && 
                                  localTask.syncStatus != "pending_delete" &&
                                  localTask.syncStatus != "pending_update") {
                            // Server has newer version and we're not in the middle of an update or delete
                            teamTaskDao.updateTask(
                                remoteTask.toEntity().copy(
                                    id = localTask.id,
                                    syncStatus = "synced",
                                    serverId = remoteTask.id
                                )
                            )
                        }
                    }
                    
                    // Update last sync timestamp
                    dataStoreManager.saveLastTeamTaskSyncTimestamp(System.currentTimeMillis())
                } else {
                    Log.e(TAG, "Failed to fetch remote tasks: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching remote tasks", e)
            }
            
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error during task sync", e)
            return Result.failure(e)
        }
    }

    override suspend fun syncTaskAssignees(): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }
        
        try {
            val pendingAssignees = teamTaskAssigneeDao.getPendingSyncAssignees()
            
            // Group by sync status
            val assigneesToCreate = pendingAssignees.filter { it.syncStatus == "pending_create" }
            val assigneesToDelete = pendingAssignees.filter { it.syncStatus == "pending_delete" }
            
            // Process creates
            for (assignee in assigneesToCreate) {
                try {
                    val response = apiService.assignUserToTask(assignee.taskId, assignee.userId)
                    
                    if (response.isSuccessful && response.body() != null) {
                        val serverAssignee = response.body()!!
                        teamTaskAssigneeDao.updateAssignee(
                            assignee.copy(
                                serverId = serverAssignee.id,
                                syncStatus = "synced",
                                lastModified = System.currentTimeMillis()
                            )
                        )
                    } else {
                        Log.e(TAG, "Failed to assign user to task: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error assigning user to task", e)
                    continue
                }
            }
            
            // Process deletes
            for (assignee in assigneesToDelete) {
                if (assignee.serverId == null) {
                    // If assignee was never synced, we can just delete it locally
                    teamTaskAssigneeDao.deleteAssignee(assignee.taskId, assignee.userId)
                    continue
                }
                
                try {
                    val response = apiService.removeUserFromTask(assignee.taskId, assignee.userId)
                    
                    if (response.isSuccessful) {
                        teamTaskAssigneeDao.deleteAssignee(assignee.taskId, assignee.userId)
                    } else {
                        Log.e(TAG, "Failed to remove user from task: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing user from task", e)
                    continue
                }
            }
            
            // Fetch and merge remote assignees
            try {
                val lastSyncTimestamp = dataStoreManager.getLastTaskAssigneeSyncTimestamp() ?: 0
                val response = apiService.getTaskAssignees(lastSyncTimestamp)
                
                if (response.isSuccessful && response.body() != null) {
                    val remoteAssignees = response.body()!!
                    
                    for (remoteAssignee in remoteAssignees) {
                        val localAssignee = teamTaskAssigneeDao.getAssigneeByServerIdSync(remoteAssignee.id)
                        
                        if (localAssignee == null) {
                            // New assignee from server
                            teamTaskAssigneeDao.insertAssignee(
                                remoteAssignee.toEntity().copy(
                                    id = UUID.randomUUID().toString(),
                                    syncStatus = "synced",
                                    serverId = remoteAssignee.id
                                )
                            )
                        } else if (remoteAssignee.lastModified > localAssignee.lastModified && 
                                   localAssignee.syncStatus != "pending_delete") {
                            // Server has newer version and we're not in the middle of a delete
                            teamTaskAssigneeDao.updateAssignee(
                                remoteAssignee.toEntity().copy(
                                    id = localAssignee.id,
                                    syncStatus = "synced",
                                    serverId = remoteAssignee.id
                                )
                            )
                        }
                    }
                    
                    // Update last sync timestamp
                    dataStoreManager.saveLastTaskAssigneeSyncTimestamp(System.currentTimeMillis())
                } else {
                    Log.e(TAG, "Failed to fetch remote assignees: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching remote assignees", e)
            }
            
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error during assignee sync", e)
            return Result.failure(e)
        }
    }
} 