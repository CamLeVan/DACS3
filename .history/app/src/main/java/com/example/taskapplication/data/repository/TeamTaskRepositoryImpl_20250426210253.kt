package com.example.taskapplication.data.repository

import android.util.Log
import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.database.dao.TeamTaskDao
import com.example.taskapplication.data.database.dao.TaskAssignmentDao
import com.example.taskapplication.data.mapper.toApiRequest
import com.example.taskapplication.data.mapper.toDomainModel
import com.example.taskapplication.data.mapper.toEntity
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.domain.model.TeamTask
import com.example.taskapplication.domain.model.Task
import com.example.taskapplication.domain.model.TaskAssignment
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
    private val assignmentDao: TaskAssignmentDao,
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager,
    private val connectionChecker: ConnectionChecker
) : TeamTaskRepository {

    private val TAG = "TeamTaskRepository"

    override fun getTeamTasks(teamId: String): Flow<List<Task>> {
        return teamTaskDao.getTasksByTeamId(teamId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getTaskById(taskId: String): Flow<Task?> {
        return teamTaskDao.getTaskById(taskId).map { it?.toDomainModel() }
    }

    override fun getTaskAssignments(taskId: String): Flow<List<TaskAssignment>> {
        return assignmentDao.getAssignmentsByTaskId(taskId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun createTask(task: Task): Result<Task> {
        if (task.teamId == null) {
            return Result.failure(IllegalArgumentException("Team task must have a teamId"))
        }

        val taskEntity = task.copy(
            id = UUID.randomUUID().toString(),
            syncStatus = "pending_create",
            lastModified = System.currentTimeMillis()
        ).toEntity()

        teamTaskDao.insertTask(taskEntity)

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTasks()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after creating task", e)
            }
        }

        return Result.success(taskEntity.toDomainModel())
    }

    override suspend fun updateTask(task: Task): Result<Task> {
        if (task.teamId == null) {
            return Result.failure(IllegalArgumentException("Team task must have a teamId"))
        }

        val existing = teamTaskDao.getTaskByIdSync(task.id)
            ?: return Result.failure(NoSuchElementException("Task not found"))

        val updatedTask = task.copy(
            syncStatus = if (existing.serverId != null) "pending_update" else "pending_create",
            lastModified = System.currentTimeMillis()
        ).toEntity()

        teamTaskDao.updateTask(updatedTask)

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTasks()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after updating task", e)
            }
        }

        return Result.success(updatedTask.toDomainModel())
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> {
        val task = teamTaskDao.getTaskByIdSync(taskId)
            ?: return Result.failure(NoSuchElementException("Task not found"))

        if (task.serverId == null) {
            // If the task has never been synced, just delete it locally
            teamTaskDao.deleteLocalOnlyTask(taskId)
            assignmentDao.deleteAssignmentsByTaskId(taskId)
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

    override suspend fun assignTaskToUser(taskId: String, userId: String): Result<TaskAssignment> {
        val task = teamTaskDao.getTaskByIdSync(taskId)
            ?: return Result.failure(NoSuchElementException("Task not found"))

        val existingAssignment = assignmentDao.getAssignmentByUserIdSync(taskId, userId)
        if (existingAssignment != null) {
            return Result.success(existingAssignment.toDomainModel())
        }

        val assignment = TaskAssignment(
            id = UUID.randomUUID().toString(),
            taskId = taskId,
            userId = userId,
            assignedAt = System.currentTimeMillis(),
            syncStatus = "pending_create",
            lastModified = System.currentTimeMillis()
        )

        assignmentDao.insertAssignment(assignment.toEntity())

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncAssignments()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after assigning task", e)
            }
        }

        return Result.success(assignment)
    }

    override suspend fun unassignTaskFromUser(taskId: String, userId: String): Result<Unit> {
        val assignment = assignmentDao.getAssignmentByUserIdSync(taskId, userId)
            ?: return Result.failure(NoSuchElementException("Assignment not found"))

        if (assignment.serverId == null) {
            // If the assignment has never been synced, just delete it locally
            assignmentDao.deleteAssignmentByUserId(taskId, userId)
        } else {
            // Mark for deletion during next sync
            assignmentDao.markAssignmentForDeletion(assignment.id, System.currentTimeMillis())
        }

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncAssignments()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after unassigning task", e)
            }
        }

        return Result.success(Unit)
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
                    val response = apiService.createTeamTask(task.teamId!!, task.toApiRequest())
                    
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
                    val response = apiService.updateTeamTask(task.teamId!!, task.serverId, task.toApiRequest())
                    
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
                if (task.serverId == null) continue
                
                try {
                    val response = apiService.deleteTeamTask(task.teamId!!, task.serverId)
                    
                    if (response.isSuccessful) {
                        teamTaskDao.deleteTask(task.id)
                        assignmentDao.deleteAssignmentsByTaskId(task.id)
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
                val response = apiService.getAllTeamTasks(lastSyncTimestamp)
                
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

    override suspend fun syncAssignments(): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        try {
            val pendingAssignments = assignmentDao.getPendingSyncAssignments()
            
            // Group by sync status
            val assignmentsToCreate = pendingAssignments.filter { it.syncStatus == "pending_create" }
            val assignmentsToDelete = pendingAssignments.filter { it.syncStatus == "pending_delete" }
            
            // Process creates
            for (assignment in assignmentsToCreate) {
                try {
                    val task = teamTaskDao.getTaskByIdSync(assignment.taskId)
                        ?: continue
                    
                    if (task.serverId == null) continue
                    
                    val response = apiService.assignTeamTask(
                        task.teamId!!, 
                        task.serverId, 
                        assignment.userId
                    )
                    
                    if (response.isSuccessful && response.body() != null) {
                        val serverAssignment = response.body()!!
                        assignmentDao.updateAssignment(
                            assignment.copy(
                                serverId = serverAssignment.id,
                                syncStatus = "synced",
                                lastModified = System.currentTimeMillis()
                            )
                        )
                    } else {
                        Log.e(TAG, "Failed to assign task: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error assigning task", e)
                    continue
                }
            }
            
            // Process deletes
            for (assignment in assignmentsToDelete) {
                if (assignment.serverId == null) continue
                
                try {
                    val task = teamTaskDao.getTaskByIdSync(assignment.taskId)
                        ?: continue
                    
                    if (task.serverId == null) continue
                    
                    val response = apiService.unassignTeamTask(
                        task.teamId!!, 
                        task.serverId, 
                        assignment.userId
                    )
                    
                    if (response.isSuccessful) {
                        assignmentDao.deleteAssignment(assignment.id)
                    } else {
                        Log.e(TAG, "Failed to unassign task: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error unassigning task", e)
                    continue
                }
            }
            
            // Fetch and merge remote assignments
            try {
                val lastSyncTimestamp = dataStoreManager.getLastAssignmentSyncTimestamp() ?: 0
                val response = apiService.getAllTaskAssignments(lastSyncTimestamp)
                
                if (response.isSuccessful && response.body() != null) {
                    val remoteAssignments = response.body()!!
                    
                    for (remoteAssignment in remoteAssignments) {
                        val localAssignment = assignmentDao.getAssignmentByServerIdSync(remoteAssignment.id)
                        
                        if (localAssignment == null) {
                            // New assignment from server
                            val taskId = teamTaskDao.getTaskByServerIdSync(remoteAssignment.taskId)?.id
                                ?: continue
                            
                            assignmentDao.insertAssignment(
                                remoteAssignment.toEntity().copy(
                                    id = UUID.randomUUID().toString(),
                                    taskId = taskId,
                                    syncStatus = "synced",
                                    serverId = remoteAssignment.id
                                )
                            )
                        } else if (remoteAssignment.lastModified > localAssignment.lastModified && 
                                  localAssignment.syncStatus != "pending_delete") {
                            // Server has newer version and we're not in the middle of a delete
                            assignmentDao.updateAssignment(
                                remoteAssignment.toEntity().copy(
                                    id = localAssignment.id,
                                    taskId = localAssignment.taskId,
                                    syncStatus = "synced",
                                    serverId = remoteAssignment.id
                                )
                            )
                        }
                    }
                    
                    // Update last sync timestamp
                    dataStoreManager.saveLastAssignmentSyncTimestamp(System.currentTimeMillis())
                } else {
                    Log.e(TAG, "Failed to fetch remote assignments: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching remote assignments", e)
            }
            
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error during assignment sync", e)
            return Result.failure(e)
        }
    }
} 