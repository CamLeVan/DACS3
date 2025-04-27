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
        return teamTaskDao.getTasksByTeam(teamId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getTeamTaskById(taskId: String): Flow<TeamTask?> {
        return teamTaskDao.getTaskById(taskId).map { it?.toDomainModel() }
    }

    override fun getTeamTasksByStatus(teamId: String, status: String): Flow<List<TeamTask>> {
        return teamTaskDao.getTasksByTeamAndStatus(teamId, status).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getTeamTasksByAssignee(teamId: String, assigneeId: String): Flow<List<TeamTask>> {
        return teamTaskDao.getTasksByTeamAndAssignee(teamId, assigneeId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun createTeamTask(task: TeamTask): Result<TeamTask> {
        val teamTaskEntity = task.copy(
            id = UUID.randomUUID().toString(),
            syncStatus = "pending_create",
            lastModified = System.currentTimeMillis()
        ).toEntity()

        teamTaskDao.insertTask(teamTaskEntity)

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
        val existingTask = teamTaskDao.getTaskByIdSync(task.id)
            ?: return Result.failure(NoSuchElementException("Team task not found"))

        val updatedEntity = task.copy(
            syncStatus = if (existingTask.serverId != null) "pending_update" else "pending_create",
            lastModified = System.currentTimeMillis(),
            serverId = existingTask.serverId
        ).toEntity()

        teamTaskDao.updateTask(updatedEntity)

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
        val task = teamTaskDao.getTaskByIdSync(taskId)
            ?: return Result.failure(NoSuchElementException("Team task not found"))

        if (task.serverId == null) {
            // If the task has never been synced, just delete it locally
            teamTaskDao.deleteLocalOnlyTask(taskId)
        } else {
            // Mark for deletion during next sync
            teamTaskDao.markTaskForDeletion(taskId, System.currentTimeMillis())
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

    override suspend fun assignTeamTask(taskId: String, assigneeId: String): Result<TeamTask> {
        val task = teamTaskDao.getTaskByIdSync(taskId)
            ?: return Result.failure(NoSuchElementException("Team task not found"))

        val updatedTask = task.copy(
            assigneeId = assigneeId,
            syncStatus = if (task.serverId != null) "pending_update" else "pending_create",
            lastModified = System.currentTimeMillis()
        )

        teamTaskDao.updateTask(updatedTask)

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTeamTasks()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after assigning team task", e)
            }
        }

        return Result.success(updatedTask.toDomainModel())
    }

    override suspend fun updateTeamTaskStatus(taskId: String, status: String): Result<TeamTask> {
        val task = teamTaskDao.getTaskByIdSync(taskId)
            ?: return Result.failure(NoSuchElementException("Team task not found"))

        val updatedTask = task.copy(
            status = status,
            syncStatus = if (task.serverId != null) "pending_update" else "pending_create",
            lastModified = System.currentTimeMillis()
        )

        teamTaskDao.updateTask(updatedTask)

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTeamTasks()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after updating team task status", e)
            }
        }

        return Result.success(updatedTask.toDomainModel())
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
                    val response = apiService.createTeamTask(task.teamId, task.toApiRequest())
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
                    val response = apiService.updateTeamTask(task.teamId, task.serverId, task.toApiRequest())
                    if (response.isSuccessful) {
                        teamTaskDao.updateTask(
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
                    val response = apiService.deleteTeamTask(task.teamId, task.serverId)
                    if (response.isSuccessful) {
                        teamTaskDao.deleteTask(task.id)
                    } else {
                        Log.e(TAG, "Failed to delete team task: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting team task", e)
                    continue
                }
            }
            
            // Fetch and merge remote tasks
            try {
                val lastSyncTimestamp = dataStoreManager.getLastTeamTaskSyncTimestamp() ?: 0
                
                // Get all teams that the user belongs to
                val teamsResponse = apiService.getTeams(0)
                if (teamsResponse.isSuccessful && teamsResponse.body() != null) {
                    val teams = teamsResponse.body()!!
                    
                    for (team in teams) {
                        val tasksResponse = apiService.getTeamTasks(team.id, lastSyncTimestamp)
                        
                        if (tasksResponse.isSuccessful && tasksResponse.body() != null) {
                            val remoteTasks = tasksResponse.body()!!
                            
                            for (remoteTask in remoteTasks) {
                                val localTask = teamTaskDao.getTaskByServerIdSync(remoteTask.id)
                                
                                // Find the local team ID for this remote team
                                val localTeam = teamTaskDao.getLocalTeamIdByServerIdSync(team.id)
                                if (localTeam == null) {
                                    Log.e(TAG, "Could not find local team for server team ID: ${team.id}")
                                    continue
                                }
                                
                                if (localTask == null) {
                                    // New task from server
                                    teamTaskDao.insertTask(
                                        remoteTask.toEntity().copy(
                                            id = UUID.randomUUID().toString(),
                                            teamId = localTeam,
                                            syncStatus = "synced",
                                            serverId = remoteTask.id
                                        )
                                    )
                                } else if (remoteTask.lastModified > localTask.lastModified && 
                                          localTask.syncStatus != "pending_update" && 
                                          localTask.syncStatus != "pending_delete") {
                                    // Server has newer version and we're not in the middle of an update
                                    teamTaskDao.updateTask(
                                        remoteTask.toEntity().copy(
                                            id = localTask.id,
                                            teamId = localTask.teamId,
                                            syncStatus = "synced",
                                            serverId = remoteTask.id
                                        )
                                    )
                                }
                            }
                        }
                    }
                    
                    // Update last sync timestamp
                    dataStoreManager.saveLastTeamTaskSyncTimestamp(System.currentTimeMillis())
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