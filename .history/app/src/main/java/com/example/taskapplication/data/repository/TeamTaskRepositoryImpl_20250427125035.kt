package com.example.taskapplication.data.repository

import android.util.Log
import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.database.dao.TeamDao
import com.example.taskapplication.data.database.dao.TeamMemberDao
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

        // Check if team exists
        val team = teamDao.getTeamByIdSync(task.teamId)
            ?: return Result.failure(NoSuchElementException("Team not found"))

        // Check if user is member of the team
        val isMember = teamMemberDao.isUserMemberOfTeam(task.teamId, currentUserId)
        if (!isMember) {
            return Result.failure(IllegalStateException("You are not a member of this team"))
        }

        val taskWithId = task.copy(
            id = UUID.randomUUID().toString(),
            createdBy = currentUserId,
            createdAt = System.currentTimeMillis(),
            syncStatus = "pending_create",
            lastModified = System.currentTimeMillis()
        )

        teamTaskDao.insertTask(taskWithId.toEntity())

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTasks()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after creating team task", e)
            }
        }

        return Result.success(taskWithId)
    }

    override suspend fun updateTask(task: TeamTask): Result<TeamTask> {
        val existingTask = teamTaskDao.getTaskById(task.id)
            ?: return Result.failure(NoSuchElementException("Task not found"))

        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        // Check if user is member of the team
        val isMember = teamMemberDao.isUserMemberOfTeam(existingTask.teamId, currentUserId)
        if (!isMember) {
            return Result.failure(IllegalStateException("You are not a member of this team"))
        }

        // Only the creator or assignee can update the task
        if (existingTask.createdBy != currentUserId && existingTask.assignedTo != currentUserId) {
            val isAdmin = teamMemberDao.isUserAdminOfTeam(existingTask.teamId, currentUserId)
            if (!isAdmin) {
                return Result.failure(IllegalStateException("Only the creator, assignee, or team admin can update this task"))
            }
        }

        val updatedTask = task.copy(
            syncStatus = if (existingTask.serverId == null) "pending_create" else "pending_update",
            lastModified = System.currentTimeMillis()
        )

        teamTaskDao.updateTask(updatedTask.toEntity())

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTasks()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after updating team task", e)
            }
        }

        return Result.success(updatedTask)
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> {
        val task = teamTaskDao.getTaskById(taskId)
            ?: return Result.failure(NoSuchElementException("Task not found"))

        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        // Check if user is creator or admin
        if (task.createdBy != currentUserId) {
            val isAdmin = teamMemberDao.isUserAdminOfTeam(task.teamId, currentUserId)
            if (!isAdmin) {
                return Result.failure(IllegalStateException("Only the creator or team admin can delete this task"))
            }
        }

        if (task.serverId == null) {
            // If task has never been synced, just delete locally
            teamTaskDao.deleteLocalOnlyTask(taskId)
        } else {
            // Mark for deletion during next sync
            teamTaskDao.markTaskForDeletion(taskId, System.currentTimeMillis())
        }

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTasks()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after deleting team task", e)
            }
        }

        return Result.success(Unit)
    }

    override suspend fun assignTask(taskId: String, userId: String?): Result<TeamTask> {
        val task = teamTaskDao.getTaskById(taskId)
            ?: return Result.failure(NoSuchElementException("Task not found"))

        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        // Check if user is member of the team
        val isMember = teamMemberDao.isUserMemberOfTeam(task.teamId, currentUserId)
        if (!isMember) {
            return Result.failure(IllegalStateException("You are not a member of this team"))
        }

        // If assigning to someone else, check if current user is task creator or admin
        if (userId != null && userId != currentUserId && task.createdBy != currentUserId) {
            val isAdmin = teamMemberDao.isUserAdminOfTeam(task.teamId, currentUserId)
            if (!isAdmin) {
                return Result.failure(IllegalStateException("Only the creator or team admin can assign this task to others"))
            }
        }

        // If assigning to someone, check if that user is a team member
        if (userId != null) {
            val isTargetMember = teamMemberDao.isUserMemberOfTeam(task.teamId, userId)
            if (!isTargetMember) {
                return Result.failure(IllegalStateException("The user you're trying to assign is not a member of this team"))
            }
        }

        val updatedTask = task.copy(
            assignedTo = userId,
            syncStatus = if (task.serverId == null) "pending_create" else "pending_update",
            lastModified = System.currentTimeMillis()
        )

        teamTaskDao.updateTask(updatedTask.toEntity())

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTasks()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after assigning team task", e)
            }
        }

        return Result.success(updatedTask.toDomainModel())
    }

    private suspend fun updateTeamTaskStatus(taskId: String, status: String): Result<TeamTask> {
        val task = teamTaskDao.getTaskById(taskId)
            ?: return Result.failure(NoSuchElementException("Task not found"))

        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        // Check if user is member of the team
        val isMember = teamMemberDao.isUserMemberOfTeam(task.teamId, currentUserId)
        if (!isMember) {
            return Result.failure(IllegalStateException("You are not a member of this team"))
        }

        // If changing status of a task assigned to someone else
        if (task.assignedTo != null && task.assignedTo != currentUserId && task.createdBy != currentUserId) {
            val isAdmin = teamMemberDao.isUserAdminOfTeam(task.teamId, currentUserId)
            if (!isAdmin) {
                return Result.failure(IllegalStateException("Only the creator, assignee, or team admin can update the status"))
            }
        }

        // Validate status
        if (status !in listOf("TODO", "IN_PROGRESS", "DONE", "BLOCKED")) {
            return Result.failure(IllegalArgumentException("Invalid status. Must be 'TODO', 'IN_PROGRESS', 'DONE', or 'BLOCKED'"))
        }

        val updatedTask = task.copy(
            status = status,
            syncStatus = if (task.serverId == null) "pending_create" else "pending_update",
            lastModified = System.currentTimeMillis()
        )

        teamTaskDao.updateTask(updatedTask.toEntity())

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTasks()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after updating team task status", e)
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
                    val team = teamDao.getTeamByIdSync(task.teamId)
                        ?: continue // Skip if team not found

                    if (team.serverId == null) {
                        continue // Skip if team is not synced yet
                    }

                    // Use server id for the team
                    val taskRequest = task.toApiRequest()
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

                    if (team.serverId == null) {
                        continue // Skip if team is not synced yet
                    }

                    // Use server id for the team
                    val taskRequest = task.toApiRequest()
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
                    Log.e(TAG, "Error updating team task", e)
                    continue
                }
            }

            // Process deletes
            for (task in tasksToDelete) {
                if (task.serverId == null) {
                    // If task was never synced, we can just delete it locally
                    teamTaskDao.deleteSyncedTask(task.id)
                    continue
                }

                try {
                    val response = apiService.deleteTeamTask(task.serverId)

                    if (response.isSuccessful) {
                        teamTaskDao.deleteSyncedTask(task.id)
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
                        // Find local team by server ID
                        val localTeam = teamDao.getTeamByServerIdSync(remoteTask.teamId)
                            ?: continue // Skip if team doesn't exist locally

                        // TODO: Implement getTaskByServerId in TeamTaskDao
                        val localTask = null // teamTaskDao.getTaskByServerId(remoteTask.id)

                        if (localTask == null) {
                            // New task from server
                            teamTaskDao.insertTask(
                                remoteTask.toEntity().copy(
                                    id = UUID.randomUUID().toString(),
                                    teamId = localTeam.id, // Use local team ID
                                    syncStatus = "synced",
                                    serverId = remoteTask.id
                                )
                            )
                        } else if (remoteTask.lastModified > localTask.lastModified &&
                                  localTask.syncStatus != "pending_update" &&
                                  localTask.syncStatus != "pending_delete") {
                            // Server has newer version and we're not in the middle of an update or delete
                            teamTaskDao.updateTask(
                                remoteTask.toEntity().copy(
                                    id = localTask.id,
                                    teamId = localTeam.id, // Use local team ID
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