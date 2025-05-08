package com.example.taskapplication.data.repository

import android.util.Log
import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.api.request.AddTeamMemberRequest
import com.example.taskapplication.data.database.dao.TeamDao
import com.example.taskapplication.data.database.dao.TeamMemberDao
import com.example.taskapplication.data.mapper.toApiRequest
import com.example.taskapplication.data.mapper.toDomainModel
import com.example.taskapplication.data.mapper.toEntity
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.domain.model.Team
import com.example.taskapplication.domain.model.TeamMember
import com.example.taskapplication.domain.repository.TeamRepository
import retrofit2.Response
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeamRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val teamDao: TeamDao,
    private val teamMemberDao: TeamMemberDao,
    private val connectionChecker: ConnectionChecker,
    private val dataStoreManager: DataStoreManager
) : TeamRepository {

    private val TAG = "TeamRepository"

    override fun getAllTeams(): Flow<List<Team>> {
        return teamDao.getAllTeams().map { teams ->
            teams.map { it.toDomainModel() }
        }
    }

    override fun getTeamsForUser(userId: String): Flow<List<Team>> {
        return teamDao.getTeamsForUser(userId).map { teams ->
            teams.map { it.toDomainModel() }
        }
    }

    override fun getTeamById(teamId: String): Flow<Team?> {
        return teamDao.getTeam(teamId).map { it?.toDomainModel() }
    }

    override suspend fun createTeam(team: Team): Result<Team> {
        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        val newTeam = team.copy(
            id = UUID.randomUUID().toString(),
            syncStatus = "pending",
            createdBy = currentUserId
        )

        return try {
            teamDao.insertTeam(newTeam.toEntity())
            Result.success(newTeam)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating team", e)
            Result.failure(e)
        }
    }

    override suspend fun updateTeam(team: Team): Result<Team> {
        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        val existingTeam = teamDao.getTeamSync(team.id) ?:
            return Result.failure(IllegalStateException("Team not found"))

        if (existingTeam.createdBy != currentUserId) {
            return Result.failure(IllegalStateException("Not authorized to update this team"))
        }

        val updatedTeam = team.copy(
            syncStatus = "pending",
            lastModified = System.currentTimeMillis()
        )

        return try {
            teamDao.updateTeam(updatedTeam.toEntity())
            Result.success(updatedTeam)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating team", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteTeam(teamId: String): Result<Unit> {
        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        val existingTeam = teamDao.getTeamSync(teamId) ?:
            return Result.failure(IllegalStateException("Team not found"))

        if (existingTeam.createdBy != currentUserId) {
            return Result.failure(IllegalStateException("Not authorized to delete this team"))
        }

        return try {
            teamDao.deleteTeam(teamId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting team", e)
            Result.failure(e)
        }
    }

    override suspend fun inviteUserToTeam(teamId: String, userId: String, role: String): Result<TeamMember> {
        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        val existingTeam = teamDao.getTeamSync(teamId) ?:
            return Result.failure(IllegalStateException("Team not found"))

        if (existingTeam.createdBy != currentUserId) {
            return Result.failure(IllegalStateException("Not authorized to invite users to this team"))
        }

        val newMember = TeamMember(
            id = UUID.randomUUID().toString(),
            teamId = teamId,
            userId = userId,
            role = role,
            syncStatus = "pending",
            createdAt = System.currentTimeMillis(),
            lastModified = System.currentTimeMillis()
        )

        return try {
            teamMemberDao.insertMember(newMember.toEntity())
            Result.success(newMember)
        } catch (e: Exception) {
            Log.e(TAG, "Error inviting user to team", e)
            Result.failure(e)
        }
    }

    override suspend fun removeUserFromTeam(teamId: String, userId: String): Result<Unit> {
        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        val existingTeam = teamDao.getTeamSync(teamId) ?:
            return Result.failure(IllegalStateException("Team not found"))

        if (existingTeam.createdBy != currentUserId) {
            return Result.failure(IllegalStateException("Not authorized to remove users from this team"))
        }

        return try {
            teamMemberDao.deleteMember(teamId, userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing user from team", e)
            Result.failure(e)
        }
    }

    override suspend fun syncTeams(): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        return try {
            // Sync pending teams
            val pendingTeams = teamDao.getPendingTeams()
            for (team in pendingTeams) {
                when (team.syncStatus) {
                    "pending_create" -> {
                        val response = apiService.createTeam(team.toApiRequest())
                        if (response.isSuccessful) {
                            val serverId = response.body()?.id.toString()
                            teamDao.updateTeamServerId(team.id, serverId)
                            teamDao.markTeamAsSynced(team.id)
                        }
                    }
                    "pending_update" -> {
                        val response = apiService.updateTeam(team.serverId!!, team.toApiRequest())
                        if (response.isSuccessful) {
                            teamDao.markTeamAsSynced(team.id)
                        }
                    }
                    "pending_delete" -> {
                        val response = apiService.deleteTeam(team.serverId!!)
                        if (response.isSuccessful) {
                            teamDao.deleteTeam(team.id)
                        }
                    }
                }
            }

            // Sync server teams
            val response = apiService.getTeams()
            if (response.isSuccessful) {
                val serverTeams = response.body() ?: emptyList()
                for (serverTeam in serverTeams) {
                    val existingTeam = teamDao.getTeamByServerId(serverTeam.id.toString())
                    if (existingTeam == null) {
                        teamDao.insertTeam(serverTeam.toEntity())
                    } else {
                        teamDao.updateTeam(serverTeam.toEntity(existingTeam))
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing teams", e)
            Result.failure(e)
        }
    }

    override suspend fun syncTeamMembers(): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        return try {
            // Sync pending members
            val pendingMembers = teamMemberDao.getPendingMembers()
            for (member in pendingMembers) {
                when (member.syncStatus) {
                    "pending_create" -> {
                        val response = apiService.addTeamMember(member.teamId, member.toApiRequest())
                        if (response.isSuccessful) {
                            val serverId = response.body()?.id.toString()
                            teamMemberDao.updateMemberServerId(member.id, serverId)
                            teamMemberDao.markMemberAsSynced(member.id)
                        }
                    }
                    "pending_update" -> {
                        val response = apiService.updateTeamMember(member.teamId, member.serverId!!, member.toApiRequest())
                        if (response.isSuccessful) {
                            teamMemberDao.markMemberAsSynced(member.id)
                        }
                    }
                    "pending_delete" -> {
                        val response = apiService.removeTeamMember(member.teamId, member.serverId!!)
                        if (response.isSuccessful) {
                            teamMemberDao.deleteMember(member.teamId, member.userId)
                        }
                    }
                }
            }

            // Sync server members
            val response = apiService.getTeamMembers()
            if (response.isSuccessful) {
                val serverMembers = response.body() ?: emptyList()
                for (serverMember in serverMembers) {
                    val existingMember = teamMemberDao.getMemberByServerId(serverMember.id.toString())
                    if (existingMember == null) {
                        teamMemberDao.insertMember(serverMember.toEntity())
                    } else {
                        teamMemberDao.updateMember(serverMember.toEntity(existingMember))
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing team members", e)
            Result.failure(e)
        }
    }
}