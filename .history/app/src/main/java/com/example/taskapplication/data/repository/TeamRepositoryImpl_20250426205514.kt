package com.example.taskapplication.data.repository

import android.util.Log
import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.api.request.AddTeamMemberRequest
import com.example.taskapplication.data.database.dao.TeamDao
import com.example.taskapplication.data.database.dao.UserDao
import com.example.taskapplication.data.mapper.toApiRequest
import com.example.taskapplication.data.mapper.toDomainModel
import com.example.taskapplication.data.mapper.toEntity
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.domain.model.Team
import com.example.taskapplication.domain.model.User
import com.example.taskapplication.domain.repository.TeamRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeamRepositoryImpl @Inject constructor(
    private val teamDao: TeamDao,
    private val userDao: UserDao,
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager,
    private val connectionChecker: ConnectionChecker
) : TeamRepository {

    private val TAG = "TeamRepository"

    override fun getAllTeams(): Flow<List<Team>> {
        return teamDao.getAllTeams().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getTeamsByOwner(ownerId: String): Flow<List<Team>> {
        return teamDao.getTeamsByOwner(ownerId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getTeamById(id: String): Team? {
        return teamDao.getTeamById(id)?.toDomainModel()
    }

    override suspend fun createTeam(team: Team): Result<Team> {
        val teamEntity = team.copy(
            id = UUID.randomUUID().toString(),
            syncStatus = "pending_create",
            lastModified = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        ).toEntity()

        teamDao.insertTeam(teamEntity)

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTeams()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after create", e)
            }
        }

        return Result.success(teamEntity.toDomainModel())
    }

    override suspend fun updateTeam(team: Team): Result<Team> {
        val existingTeam = teamDao.getTeamById(team.id)
            ?: return Result.failure(NoSuchElementException("Team not found"))

        val updatedEntity = team.copy(
            syncStatus = if (existingTeam.serverId != null) "pending_update" else "pending_create",
            lastModified = System.currentTimeMillis(),
            serverId = existingTeam.serverId,
            createdAt = existingTeam.createdAt
        ).toEntity()

        teamDao.updateTeam(updatedEntity)

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTeams()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after update", e)
            }
        }

        return Result.success(updatedEntity.toDomainModel())
    }

    override suspend fun deleteTeam(teamId: String): Result<Unit> {
        val team = teamDao.getTeamById(teamId)
            ?: return Result.failure(NoSuchElementException("Team not found"))

        if (team.serverId == null) {
            // If the team has never been synced, just delete it locally
            teamDao.deleteLocalOnlyTeam(teamId)
        } else {
            // Mark for deletion during next sync
            teamDao.markTeamForDeletion(teamId, System.currentTimeMillis())
        }

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTeams()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after delete", e)
            }
        }

        return Result.success(Unit)
    }

    override suspend fun getTeamMembers(teamId: String): Result<List<User>> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        val team = teamDao.getTeamById(teamId)
            ?: return Result.failure(NoSuchElementException("Team not found"))

        if (team.serverId == null) {
            return Result.failure(IOException("Team not synced with server"))
        }

        try {
            val response = apiService.getTeamMembers(team.serverId)
            if (!response.isSuccessful || response.body() == null) {
                return Result.failure(IOException("Failed to get team members: ${response.message()}"))
            }

            val memberResponses = response.body()!!
            val members = memberResponses.map { userResponse ->
                val userEntity = userResponse.toEntity()
                userDao.insertUser(userEntity) // Cache user data
                userEntity.toDomainModel()
            }

            return Result.success(members)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting team members", e)
            return Result.failure(e)
        }
    }

    override suspend fun addTeamMember(teamId: String, userId: String, role: String): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        val team = teamDao.getTeamById(teamId)
            ?: return Result.failure(NoSuchElementException("Team not found"))

        if (team.serverId == null) {
            return Result.failure(IOException("Team not synced with server"))
        }

        val user = userDao.getUserById(userId)
            ?: return Result.failure(NoSuchElementException("User not found"))

        if (user.serverId == null) {
            return Result.failure(IOException("User not synced with server"))
        }

        try {
            val request = AddTeamMemberRequest(user.serverId, role)
            val response = apiService.addTeamMember(team.serverId, request)

            if (!response.isSuccessful) {
                return Result.failure(IOException("Failed to add team member: ${response.message()}"))
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding team member", e)
            return Result.failure(e)
        }
    }

    override suspend fun removeTeamMember(teamId: String, userId: String): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        val team = teamDao.getTeamById(teamId)
            ?: return Result.failure(NoSuchElementException("Team not found"))

        if (team.serverId == null) {
            return Result.failure(IOException("Team not synced with server"))
        }

        val user = userDao.getUserById(userId)
            ?: return Result.failure(NoSuchElementException("User not found"))

        if (user.serverId == null) {
            return Result.failure(IOException("User not synced with server"))
        }

        try {
            val response = apiService.removeTeamMember(team.serverId, user.serverId)

            if (!response.isSuccessful) {
                return Result.failure(IOException("Failed to remove team member: ${response.message()}"))
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing team member", e)
            return Result.failure(e)
        }
    }

    override suspend fun syncTeams(): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        try {
            val pendingTeams = teamDao.getPendingSyncTeams()
            
            // Group by sync status
            val teamsToCreate = pendingTeams.filter { it.syncStatus == "pending_create" }
            val teamsToUpdate = pendingTeams.filter { it.syncStatus == "pending_update" }
            val teamsToDelete = pendingTeams.filter { it.syncStatus == "pending_delete" }
            
            // Process creates
            for (team in teamsToCreate) {
                try {
                    val response = apiService.createTeam(team.toApiRequest())
                    if (response.isSuccessful && response.body() != null) {
                        val serverTeam = response.body()!!
                        teamDao.insertTeam(
                            team.copy(
                                serverId = serverTeam.id,
                                syncStatus = "synced",
                                lastModified = System.currentTimeMillis()
                            )
                        )
                    } else {
                        Log.e(TAG, "Failed to create team: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating team", e)
                    continue
                }
            }
            
            // Process updates
            for (team in teamsToUpdate) {
                if (team.serverId == null) continue
                
                try {
                    val response = apiService.updateTeam(team.serverId, team.toApiRequest())
                    if (response.isSuccessful) {
                        teamDao.insertTeam(
                            team.copy(
                                syncStatus = "synced",
                                lastModified = System.currentTimeMillis()
                            )
                        )
                    } else {
                        Log.e(TAG, "Failed to update team: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating team", e)
                    continue
                }
            }
            
            // Process deletes
            for (team in teamsToDelete) {
                if (team.serverId == null) continue
                
                try {
                    val response = apiService.deleteTeam(team.serverId)
                    if (response.isSuccessful) {
                        teamDao.deleteTeam(team.id)
                    } else {
                        Log.e(TAG, "Failed to delete team: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting team", e)
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