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

    override fun getAllTeamTasks(teamId: String): Flow<List<TeamTask>> {
        return teamTaskDao.getTasksByTeam(teamId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getAssignedTeamTasks(memberId: String): Flow<List<TeamTask>> {
        return teamTaskDao.getTasksByAssignee(memberId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getTeamTaskById(id: String): TeamTask? {
        return teamTaskDao.getTeamTaskById(id)?.toDomainModel()
    }

    override suspend fun createTeamTask(task: TeamTask): Result<TeamTask> {
        val taskEntity = task.copy(
            id = UUID.randomUUID().toString(),
            syncStatus = "pending_create",
            lastModified = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        ).toEntity()

        teamTaskDao.insertTeamTask(taskEntity)

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTeamTasks()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after create", e)
            }
        }

        return Result.success(taskEntity.toDomainModel())
    }

    override suspend fun updateTeamTask(task: TeamTask): Result<TeamTask> {
        val existingTask = teamTaskDao.getTeamTaskById(task.id)
            ?: return Result.failure(NoSuchElementException("Team task not found"))

        val updatedEntity = task.copy(
            syncStatus = if (existingTask.serverId != null) "pending_update" else "pending_create",
            lastModified = System.currentTimeMillis(),
            serverId = existingTask.serverId,
            createdAt = existingTask.createdAt
        ).toEntity()

        teamTaskDao.updateTeamTask(updatedEntity)

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTeamTasks()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after update", e)
            }
        }

        return Result.success(updatedEntity.toDomainModel())
    }

    override suspend fun assignTeamTask(taskId: String, assigneeId: String): Result<Unit> {
        val existingTask = teamTaskDao.getTeamTaskById(taskId)
            ?: return Result.failure(NoSuchElementException("Team task not found"))

        val updatedEntity = existingTask.copy(
            assigneeId = assigneeId,
            syncStatus = if (existingTask.serverId != null) "pending_update" else "pending_create",
            lastModified = System.currentTimeMillis()
        )

        teamTaskDao.updateTeamTask(updatedEntity)

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTeamTasks()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after assignment", e)
            }
        }

        return Result.success(Unit)
    }

    override suspend fun changeTaskStatus(taskId: String, newStatus: String): Result<Unit> {
        val existingTask = teamTaskDao.getTeamTaskById(taskId)
            ?: return Result.failure(NoSuchElementException("Team task not found"))

        val updatedEntity = existingTask.copy(
            status = newStatus,
            syncStatus = if (existingTask.serverId != null) "pending_update" else "pending_create",
            lastModified = System.currentTimeMillis()
        )

        teamTaskDao.updateTeamTask(updatedEntity)

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTeamTasks()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after status change", e)
            }
        }

        return Result.success(Unit)
    }

    override suspend fun deleteTeamTask(taskId: String): Result<Unit> {
        val task = teamTaskDao.getTeamTaskById(taskId)
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
                Log.e(TAG, "Error syncing after delete", e)
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
                        teamTaskDao.insertTeamTask(
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
                        teamTaskDao.insertTeamTask(
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
            
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error during sync", e)
            return Result.failure(e)
        }
    }
} 