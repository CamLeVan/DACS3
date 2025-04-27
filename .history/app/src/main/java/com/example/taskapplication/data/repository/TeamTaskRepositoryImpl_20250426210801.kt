package com.example.taskapplication.data.repository

import android.util.Log
import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.database.dao.TeamTaskDao
import com.example.taskapplication.data.database.dao.TeamMemberDao
import com.example.taskapplication.data.mapper.toApiRequest
import com.example.taskapplication.data.mapper.toDomainModel
import com.example.taskapplication.data.mapper.toEntity
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.domain.model.TaskPriority
import com.example.taskapplication.domain.model.TaskStatus
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
    private val teamTaskDao: TeamTaskDao,
    private val teamMemberDao: TeamMemberDao,
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

    override fun getTaskById(taskId: String): Flow<TeamTask?> {
        return teamTaskDao.getTaskById(taskId).map { entity ->
            entity?.toDomainModel()
        }
    }

    override suspend fun createTask(task: TeamTask): Result<TeamTask> {
        // Validate that the task belongs to a team
        if (task.teamId.isNullOrEmpty()) {
            return Result.failure(IllegalArgumentException("Team task must have a teamId"))
        }

        // Validate that the current user is a member of the team
        val currentUserId = dataStoreManager.getCurrentUserId() ?: 
            return Result.failure(IllegalStateException("No current user found"))
            
        val isMember = teamMemberDao.isUserMemberOfTeam(task.teamId, currentUserId)
        if (!isMember) {
            return Result.failure(IllegalStateException("User is not a member of this team"))
        }

        val taskWithId = task.copy(
            id = UUID.randomUUID().toString(),
            createdBy = currentUserId,
            status = task.status ?: TaskStatus.TODO,
            priority = task.priority ?: TaskPriority.MEDIUM,
            syncStatus = "pending_create",
            lastModified = System.currentTimeMillis()
        )
        
        teamTaskDao.insertTask(taskWithId.toEntity())
        
        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTasks()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after creating task", e)
            }
        }
        
        return Result.success(taskWithId)
    }

    override suspend fun updateTask(task: TeamTask): Result<TeamTask> {
        val existingTask = teamTaskDao.getTaskByIdSync(task.id)
            ?: return Result.failure(NoSuchElementException("Task not found"))
        
        // Validate that the current user is a member of the team
        val currentUserId = dataStoreManager.getCurrentUserId() ?: 
            return Result.failure(IllegalStateException("No current user found"))
            
        val isMember = teamMemberDao.isUserMemberOfTeam(existingTask.teamId, currentUserId)
        if (!isMember) {
            return Result.failure(IllegalStateException("User is not a member of this team"))
        }
        
        val updatedTask = task.copy(
            syncStatus = if (existingTask.serverId != null) "pending_update" else "pending_create",
            lastModified = System.currentTimeMillis()
        )
        
        teamTaskDao.updateTask(updatedTask.toEntity())
        
        // If connected, try to sync immediately
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
        val task = teamTaskDao.getTaskByIdSync(taskId)
            ?: return Result.failure(NoSuchElementException("Task not found"))
        
        // Validate that the current user is a member of the team
        val currentUserId = dataStoreManager.getCurrentUserId() ?: 
            return Result.failure(IllegalStateException("No current user found"))
            
        val isMember = teamMemberDao.isUserMemberOfTeam(task.teamId, currentUserId)
        if (!isMember) {
            return Result.failure(IllegalStateException("User is not a member of this team"))
        }
        
        if (task.serverId == null) {
            // If task has never been synced, just delete locally
            teamTaskDao.deleteTask(taskId)
        } else {
            // Mark for deletion during next sync
            teamTaskDao.markTaskForDeletion(taskId, System.currentTimeMillis())
        }
        
        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTasks()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after deleting task", e)
            }
        }
        
        return Result.success(Unit)
    }

    override suspend fun assignTask(taskId: String, assigneeId: String): Result<TeamTask> {
        val task = teamTaskDao.getTaskByIdSync(taskId)
            ?: return Result.failure(NoSuchElementException("Task not found"))
        
        // Validate that both users are members of the team
        val currentUserId = dataStoreManager.getCurrentUserId() ?: 
            return Result.failure(IllegalStateException("No current user found"))
            
        val isCurrentUserMember = teamMemberDao.isUserMemberOfTeam(task.teamId, currentUserId)
        if (!isCurrentUserMember) {
            return Result.failure(IllegalStateException("Current user is not a member of this team"))
        }
        
        val isAssigneeMember = teamMemberDao.isUserMemberOfTeam(task.teamId, assigneeId)
        if (!isAssigneeMember) {
            return Result.failure(IllegalStateException("Assignee is not a member of this team"))
        }
        
        val updatedTask = task.toDomainModel().copy(
            assignedTo = assigneeId,
            syncStatus = if (task.serverId != null) "pending_update" else "pending_create",
            lastModified = System.currentTimeMillis()
        )
        
        teamTaskDao.updateTask(updatedTask.toEntity())
        
        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTasks()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after assigning task", e)
            }
        }
        
        return Result.success(updatedTask)
    }

    override suspend fun updateTaskStatus(taskId: String, status: TaskStatus): Result<TeamTask> {
        val task = teamTaskDao.getTaskByIdSync(taskId)
            ?: return Result.failure(NoSuchElementException("Task not found"))
        
        // Validate that the current user is a member of the team
        val currentUserId = dataStoreManager.getCurrentUserId() ?: 
            return Result.failure(IllegalStateException("No current user found"))
            
        val isMember = teamMemberDao.isUserMemberOfTeam(task.teamId, currentUserId)
        if (!isMember) {
            return Result.failure(IllegalStateException("User is not a member of this team"))
        }
        
        val updatedTask = task.toDomainModel().copy(
            status = status,
            syncStatus = if (task.serverId != null) "pending_update" else "pending_create",
            lastModified = System.currentTimeMillis()
        )
        
        teamTaskDao.updateTask(updatedTask.toEntity())
        
        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTasks()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after updating task status", e)
            }
        }
        
        return Result.success(updatedTask)
    }

    override suspend fun syncTasks(): Result<Unit> {
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
                if (task.serverId == null) {
                    Log.e(TAG, "Task marked for update has no serverId: ${task.id}")
                    continue
                }
                
                try {
                    val response = apiService.updateTeamTask(task.serverId, task.toApiRequest())
                    
                    if (response.isSuccessful) {
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
                                  localTask.syncStatus != "pending_delete") {
                            // Server has newer version and we're not in the middle of a delete
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
} 