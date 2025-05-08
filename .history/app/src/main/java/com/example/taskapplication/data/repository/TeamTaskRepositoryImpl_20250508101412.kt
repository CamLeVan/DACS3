package com.example.taskapplication.data.repository

import android.util.Log
import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.database.dao.TeamDao
import com.example.taskapplication.data.database.dao.TeamMemberDao
import com.example.taskapplication.data.database.dao.TeamTaskDao
import com.example.taskapplication.data.database.entities.TeamTaskEntity
import com.example.taskapplication.data.api.request.TeamTaskRequest
import com.example.taskapplication.data.api.response.TeamTaskResponse
import com.example.taskapplication.data.mapper.toDomainModel
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.domain.model.TeamTask
import com.example.taskapplication.domain.repository.TeamTaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeamTaskRepositoryImpl @Inject constructor(
    private val teamTaskDao: TeamTaskDao,
    private val teamDao: TeamDao,
    private val teamMemberDao: TeamMemberDao,
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager,
    private val connectionChecker: ConnectionChecker
) : TeamTaskRepository {

    private val TAG = "TeamTaskRepository"

    override fun getAllTeamTasks(): Flow<List<TeamTask>> {
        return teamTaskDao.getAllTeamTasks().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getTasksByTeam(teamId: String): Flow<List<TeamTask>> {
        return teamTaskDao.getTasksByTeam(teamId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getTasksAssignedToUser(userId: String): Flow<List<TeamTask>> {
        return teamTaskDao.getTasksAssignedToUser(userId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getTaskById(id: String): TeamTask? {
        return teamTaskDao.getTaskById(id)?.toDomainModel()
    }

    override suspend fun createTask(task: TeamTask): Result<TeamTask> {
        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        val taskWithId = task.copy(
            id = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis(),
            syncStatus = "pending_create",
            lastModified = System.currentTimeMillis()
        )

        teamTaskDao.insertTask(taskWithId.toEntity())

        // Try to sync immediately if online
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
        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        val existingTask = teamTaskDao.getTaskById(task.id)
            ?: return Result.failure(NoSuchElementException("Task not found"))

        // Only the assignee can update the task
        if (existingTask.assignedUserId != currentUserId) {
            val isAdmin = teamMemberDao.isUserAdminOfTeam(existingTask.teamId, currentUserId)
            if (!isAdmin) {
                return Result.failure(IllegalStateException("Only the assignee or team admin can update this task"))
            }
        }

        val updatedTask = task.copy(
            syncStatus = if (task.serverId == null) "pending_create" else "pending_update",
            lastModified = System.currentTimeMillis()
        )

        teamTaskDao.updateTask(updatedTask.toEntity())

        // Try to sync immediately if online
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

        val task = teamTaskDao.getTaskById(taskId)
            ?: return Result.failure(NoSuchElementException("Task not found"))

        // Check if user is admin
        val isAdmin = teamMemberDao.isUserAdminOfTeam(task.teamId, currentUserId)
        if (!isAdmin) {
            return Result.failure(IllegalStateException("Only the team admin can delete this task"))
        }

        if (task.serverId == null) {
            // If task was never synced, we can just delete it
            teamTaskDao.deleteTask(taskId)
        } else {
            // Otherwise mark for deletion to sync later
            teamTaskDao.markTaskForDeletion(taskId, System.currentTimeMillis())
        }

        // Try to sync immediately if online
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTasks()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after deleting task", e)
            }
        }

        return Result.success(Unit)
    }

    override suspend fun assignTask(taskId: String, userId: String?): Result<TeamTask> {
        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        val task = teamTaskDao.getTaskById(taskId)
            ?: return Result.failure(NoSuchElementException("Task not found"))

        // If assigning to someone else, check if current user is admin
        if (userId != null && userId != currentUserId) {
            val isAdmin = teamMemberDao.isUserAdminOfTeam(task.teamId, currentUserId)
            if (!isAdmin) {
                return Result.failure(IllegalStateException("Only the team admin can assign this task to others"))
            }
        }

        // Check if user is a member of the team
        if (userId != null) {
            val isMember = teamMemberDao.isUserMemberOfTeam(task.teamId, userId)
            if (!isMember) {
                return Result.failure(IllegalStateException("User is not a member of this team"))
            }
        }

        val updatedTask = task.copy(
            assignedUserId = userId,
            syncStatus = if (task.serverId == null) "pending_create" else "pending_update",
            lastModified = System.currentTimeMillis()
        )

        teamTaskDao.updateTask(updatedTask.toEntity())

        // Try to sync immediately if online
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTasks()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after assigning task", e)
            }
        }

        return Result.success(updatedTask.toDomainModel())
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
                    val request = TeamTaskRequest(
                        title = task.title,
                        description = task.description,
                        assigned_user_id = task.assignedUserId,
                        due_date = task.dueDate,
                        priority = task.priority,
                        is_completed = task.isCompleted
                    )

                    val response = apiService.createTeamTask(task.teamId.toLong(), request)

                    if (response.isSuccessful && response.body() != null) {
                        val serverTask = response.body()!!
                        teamTaskDao.updateTaskServerId(task.id, serverTask.id.toString())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating task on server", e)
                }
            }

            // Process updates
            for (task in tasksToUpdate) {
                try {
                    if (task.serverId == null) continue

                    val request = TeamTaskRequest(
                        title = task.title,
                        description = task.description,
                        assigned_user_id = task.assignedUserId,
                        due_date = task.dueDate,
                        priority = task.priority,
                        is_completed = task.isCompleted
                    )

                    val response = apiService.updateTeamTask(
                        teamId = task.teamId.toLong(),
                        taskId = task.serverId.toLong(),
                        task = request
                    )

                    if (response.isSuccessful) {
                        teamTaskDao.markTaskAsSynced(task.id)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating task on server", e)
                }
            }

            // Process deletes
            for (task in tasksToDelete) {
                try {
                    if (task.serverId == null) {
                        teamTaskDao.deleteTask(task.id)
                        continue
                    }

                    val response = apiService.deleteTeamTask(
                        teamId = task.teamId.toLong(),
                        taskId = task.serverId.toLong()
                    )

                    if (response.isSuccessful) {
                        teamTaskDao.deleteTask(task.id)
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
}
