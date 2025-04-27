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
        try {
            val currentUserId = "current_user_id" // TODO: Implement proper user management
                // ?: return Result.failure(IllegalStateException("User not logged in"))

            val taskWithId = task.copy(
                id = UUID.randomUUID().toString(),
                createdAt = System.currentTimeMillis(),
                syncStatus = "pending_create",
                lastModified = System.currentTimeMillis()
            )

            // Convert domain model to entity
            val taskEntity = TeamTaskEntity(
                id = taskWithId.id,
                teamId = taskWithId.teamId,
                title = taskWithId.title,
                description = taskWithId.description,
                assignedUserId = taskWithId.assignedUserId,
                dueDate = taskWithId.dueDate,
                priority = taskWithId.priority,
                isCompleted = taskWithId.isCompleted,
                serverId = taskWithId.serverId,
                syncStatus = taskWithId.syncStatus,
                lastModified = taskWithId.lastModified,
                createdAt = taskWithId.createdAt
            )
            teamTaskDao.insertTask(taskEntity)

            // Try to sync immediately if online
            if (connectionChecker.isNetworkAvailable()) {
                syncTasks()
            }

            return Result.success(taskWithId)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating team task", e)
            return Result.failure(e)
        }
    }

    override suspend fun updateTask(task: TeamTask): Result<TeamTask> {
        try {
            val currentUserId = "current_user_id" // TODO: Implement proper user management
                // ?: return Result.failure(IllegalStateException("User not logged in"))

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

            // Convert domain model to entity
            val taskEntity = TeamTaskEntity(
                id = updatedTask.id,
                teamId = updatedTask.teamId,
                title = updatedTask.title,
                description = updatedTask.description,
                assignedUserId = updatedTask.assignedUserId,
                dueDate = updatedTask.dueDate,
                priority = updatedTask.priority,
                isCompleted = updatedTask.isCompleted,
                serverId = updatedTask.serverId,
                syncStatus = updatedTask.syncStatus,
                lastModified = updatedTask.lastModified,
                createdAt = updatedTask.createdAt
            )
            teamTaskDao.updateTask(taskEntity)

            // Try to sync immediately if online
            if (connectionChecker.isNetworkAvailable()) {
                syncTasks()
            }

            return Result.success(updatedTask)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating team task", e)
            return Result.failure(e)
        }
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> {
        try {
            val currentUserId = "current_user_id" // TODO: Implement proper user management
                // ?: return Result.failure(IllegalStateException("User not logged in"))

            val task = teamTaskDao.getTaskById(taskId)
                ?: return Result.failure(NoSuchElementException("Task not found"))

            // Check if user is admin
            val isAdmin = teamMemberDao.isUserAdminOfTeam(task.teamId, currentUserId)
            if (!isAdmin) {
                return Result.failure(IllegalStateException("Only the team admin can delete this task"))
            }

            if (task.serverId == null) {
                // If task was never synced, we can just delete it
                teamTaskDao.deleteLocalOnlyTask(taskId)
            } else {
                // Otherwise mark for deletion to sync later
                teamTaskDao.markTaskForDeletion(taskId, System.currentTimeMillis())
            }

            // Try to sync immediately if online
            if (connectionChecker.isNetworkAvailable()) {
                syncTasks()
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting team task", e)
            return Result.failure(e)
        }
    }

    override suspend fun assignTask(taskId: String, userId: String?): Result<TeamTask> {
        try {
            val currentUserId = "current_user_id" // TODO: Implement proper user management
                // ?: return Result.failure(IllegalStateException("User not logged in"))

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

            // Convert domain model to entity
            val taskEntity = TeamTaskEntity(
                id = updatedTask.id,
                teamId = updatedTask.teamId,
                title = updatedTask.title,
                description = updatedTask.description,
                assignedUserId = updatedTask.assignedUserId,
                dueDate = updatedTask.dueDate,
                priority = updatedTask.priority,
                isCompleted = updatedTask.isCompleted,
                serverId = updatedTask.serverId,
                syncStatus = updatedTask.syncStatus,
                lastModified = updatedTask.lastModified,
                createdAt = updatedTask.createdAt
            )
            teamTaskDao.updateTask(taskEntity)

            // Try to sync immediately if online
            if (connectionChecker.isNetworkAvailable()) {
                syncTasks()
            }

            return Result.success(updatedTask.toDomainModel())
        } catch (e: Exception) {
            Log.e(TAG, "Error assigning team task", e)
            return Result.failure(e)
        }
    }

    // This method is not in the interface, but we'll keep it for future use
    suspend fun updateTaskStatus(taskId: String, status: String): Result<TeamTask> {
        try {
            val currentUserId = "current_user_id" // TODO: Implement proper user management
                // ?: return Result.failure(IllegalStateException("User not logged in"))

            val task = teamTaskDao.getTaskById(taskId)
                ?: return Result.failure(NoSuchElementException("Task not found"))

            // If changing status of a task assigned to someone else
            if (task.assignedUserId != null && task.assignedUserId != currentUserId) {
                val isAdmin = teamMemberDao.isUserAdminOfTeam(task.teamId, currentUserId)
                if (!isAdmin) {
                    return Result.failure(IllegalStateException("Only the assignee or team admin can update the status"))
                }
            }

            val updatedTask = task.copy(
                isCompleted = status == "DONE",
                syncStatus = if (task.serverId == null) "pending_create" else "pending_update",
                lastModified = System.currentTimeMillis()
            )

            // Convert domain model to entity
            val taskEntity = TeamTaskEntity(
                id = updatedTask.id,
                teamId = updatedTask.teamId,
                title = updatedTask.title,
                description = updatedTask.description,
                assignedUserId = updatedTask.assignedUserId,
                dueDate = updatedTask.dueDate,
                priority = updatedTask.priority,
                isCompleted = updatedTask.isCompleted,
                serverId = updatedTask.serverId,
                syncStatus = updatedTask.syncStatus,
                lastModified = updatedTask.lastModified,
                createdAt = updatedTask.createdAt
            )
            teamTaskDao.updateTask(taskEntity)

            // Try to sync immediately if online
            if (connectionChecker.isNetworkAvailable()) {
                syncTasks()
            }

            return Result.success(updatedTask.toDomainModel())
        } catch (e: Exception) {
            Log.e(TAG, "Error updating team task status", e)
            return Result.failure(e)
        }
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
                    val team = teamDao.getTeamByIdSync(task.teamId)
                        ?: continue // Skip if team not found

                    // Skip server sync if team doesn't have a server ID
                    if (team.serverId == null) {
                        continue // Skip if team is not synced yet
                    }

                    try {
                        // Use server id for the team
                        // TODO: Implement toApiRequest for TeamTaskEntity
                        val taskRequest = TeamTaskRequest(
                            title = task.title,
                            description = task.description,
                            assignedUserId = task.assignedUserId?.toLongOrNull(),
                            dueDate = task.dueDate,
                            priority = task.priority,
                            isCompleted = task.isCompleted
                        )
                        val response = apiService.createTeamTask(team.serverId!!.toLong(), taskRequest)

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
                        Log.e(TAG, "Error creating team task on server", e)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating team task", e)
                    continue
                }
            }

            // Process updates
            for (task in tasksToUpdate) {
                try {
                    if (task.serverId == null) {
                        continue // Skip if task is not synced yet
                    }

                    val team = teamDao.getTeamByIdSync(task.teamId)
                        ?: continue // Skip if team not found

                    // Skip server sync if team doesn't have a server ID
                    if (team.serverId == null) {
                        continue // Skip if team is not synced yet
                    }

                    try {
                        // Use server id for the team
                        // TODO: Implement toApiRequest for TeamTaskEntity
                        val taskRequest = TeamTaskRequest(
                            title = task.title,
                            description = task.description,
                            assignedUserId = task.assignedUserId?.toLongOrNull(),
                            dueDate = task.dueDate,
                            priority = task.priority,
                            isCompleted = task.isCompleted
                        )
                        val response = apiService.updateTeamTask(team.serverId!!.toLong(), task.serverId!!.toLong(), taskRequest)

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
                        Log.e(TAG, "Error updating team task on server", e)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating team task", e)
                    continue
                }
            }

            // Process deletes
            for (task in tasksToDelete) {
                if (task.serverId == null) {
                    // If task was never synced, we can just delete it locally
                    teamTaskDao.deleteTeamTask(task.id)
                    continue
                }

                try {
                    val team = teamDao.getTeamByIdSync(task.teamId)
                        ?: continue // Skip if team not found

                    if (team.serverId == null) {
                        continue // Skip if team is not synced yet
                    }

                    val response = apiService.deleteTeamTask(team.serverId!!.toLong(), task.serverId!!.toLong())

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
                // Skip fetching remote tasks for now
                // TODO: Implement proper API call when API is ready
                Log.d(TAG, "Skipping remote task fetch until API is ready")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching remote team tasks", e)
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error during team task sync", e)
            return Result.failure(e)
        }
    }

    override suspend fun syncTasksByTeam(teamId: String): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        try {
            val pendingTasks = teamTaskDao.getPendingSyncTasksByTeam(teamId)

            // Group by sync status
            val tasksToCreate = pendingTasks.filter { it.syncStatus == "pending_create" }
            val tasksToUpdate = pendingTasks.filter { it.syncStatus == "pending_update" }
            val tasksToDelete = pendingTasks.filter { it.syncStatus == "pending_delete" }

            // Process creates, updates, and deletes similar to syncTeamTasks()
            // but only for the specific team

            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error during team task sync for team $teamId", e)
            return Result.failure(e)
        }
    }
}
