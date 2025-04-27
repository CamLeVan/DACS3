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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeamTaskRepositoryImpl @Inject constructor(
    private val teamTaskDao: TeamTaskDao,
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager,
    private val connectionChecker: ConnectionChecker
) : TeamTaskRepository {

    private val TAG = "TeamTaskRepository"

    override fun getTeamTasks(teamId: String): Flow<List<TeamTask>> {
        return teamTaskDao.getTeamTasks(teamId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getTeamTask(taskId: String): Flow<TeamTask?> {
        return teamTaskDao.getTeamTaskById(taskId).map { entity ->
            entity?.toDomainModel()
        }
    }

    override suspend fun createTeamTask(task: TeamTask): Result<TeamTask> {
        val teamTaskEntity = task.copy(
            id = UUID.randomUUID().toString(),
            syncStatus = "pending_create",
            lastModified = System.currentTimeMillis()
        ).toEntity()

        teamTaskDao.insertTeamTask(teamTaskEntity)

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTeamTasks()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after creating team task", e)
            }
        }

        return Result.success(teamTaskEntity.toDomainModel())
    }

    override suspend fun updateTeamTask(task: TeamTask): Result<TeamTask> {
        val existingTask = teamTaskDao.getTeamTaskByIdSync(task.id)
            ?: return Result.failure(NoSuchElementException("Team task not found"))

        val updatedEntity = task.copy(
            syncStatus = if (existingTask.serverId != null) "pending_update" else "pending_create",
            lastModified = System.currentTimeMillis(),
            serverId = existingTask.serverId
        ).toEntity()

        teamTaskDao.updateTeamTask(updatedEntity)

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTeamTasks()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after updating team task", e)
            }
        }

        return Result.success(updatedEntity.toDomainModel())
    }

    override suspend fun deleteTeamTask(taskId: String): Result<Unit> {
        val task = teamTaskDao.getTeamTaskByIdSync(taskId)
            ?: return Result.failure(NoSuchElementException("Team task not found"))

        if (task.serverId == null) {
            // If the task has never been synced, just delete it locally
            teamTaskDao.deleteLocalOnlyTeamTask(taskId)
        } else {
            // Mark for deletion during next sync
            teamTaskDao.markTeamTaskForDeletion(taskId, System.currentTimeMillis())
        }

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTeamTasks()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after deleting team task", e)
            }
        }

        return Result.success(Unit)
    }

    override suspend fun syncTeamTasks(): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        try {
            val pendingTasks = teamTaskDao.getPendingSyncTeamTasks()
            
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
                        teamTaskDao.updateTeamTask(
                            task.copy(
                                serverId = serverTask.id,
                                syncStatus = "synced",
                                lastModified = System.currentTimeMillis()
                            )
                        )
                    } else {
                        Log.e(TAG, "Failed to create team task: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating team task", e)
                    continue
                }
            }
            
            // Process updates
            for (task in tasksToUpdate) {
                if (task.serverId == null) continue
                
                try {
                    val response = apiService.updateTeamTask(task.serverId, task.toApiRequest())
                    if (response.isSuccessful) {
                        teamTaskDao.updateTeamTask(
                            task.copy(
                                syncStatus = "synced",
                                lastModified = System.currentTimeMillis()
                            )
                        )
                    } else {
                        Log.e(TAG, "Failed to update team task: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating team task", e)
                    continue
                }
            }
            
            // Process deletes
            for (task in tasksToDelete) {
                if (task.serverId == null) continue
                
                try {
                    val response = apiService.deleteTeamTask(task.serverId)
                    if (response.isSuccessful) {
                        teamTaskDao.deleteTeamTask(task.id)
                    } else {
                        Log.e(TAG, "Failed to delete team task: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting team task", e)
                    continue
                }
            }
            
            // Fetch and merge remote team tasks
            try {
                val lastSyncTimestamp = dataStoreManager.getLastTeamTaskSyncTimestamp() ?: 0
                val response = apiService.getTeamTasks(lastSyncTimestamp)
                
                if (response.isSuccessful && response.body() != null) {
                    val remoteTasks = response.body()!!
                    
                    for (remoteTask in remoteTasks) {
                        val localTask = teamTaskDao.getTeamTaskByServerIdSync(remoteTask.id)
                        
                        if (localTask == null) {
                            // New task from server
                            teamTaskDao.insertTeamTask(
                                remoteTask.toEntity().copy(
                                    id = UUID.randomUUID().toString(),
                                    syncStatus = "synced",
                                    serverId = remoteTask.id
                                )
                            )
                        } else if (remoteTask.lastModified > localTask.lastModified && 
                                  localTask.syncStatus != "pending_update" && 
                                  localTask.syncStatus != "pending_delete") {
                            // Server has newer version and we're not in the middle of an update
                            teamTaskDao.updateTeamTask(
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
                    Log.e(TAG, "Failed to fetch remote team tasks: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching remote team tasks", e)
            }
            
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error during team task sync", e)
            return Result.failure(e)
        }
    }
} 