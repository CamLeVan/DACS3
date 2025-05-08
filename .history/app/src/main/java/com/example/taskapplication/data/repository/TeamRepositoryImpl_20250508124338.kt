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
import com.example.taskapplication.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
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
    private val dataStoreManager: DataStoreManager,
    private val networkUtils: NetworkUtils
) : TeamRepository {

    private val TAG = "TeamRepository"

    override fun getAllTeams(): Flow<List<Team>> {
        return teamDao.getAllTeams().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getTeamsForUser(userId: String): Flow<List<Team>> {
        return teamDao.getTeamsForUser(userId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getTeamById(teamId: String): Flow<Team?> {
        return teamDao.getTeamById(teamId).map { entity ->
            entity?.toDomainModel()
        }
    }

    override fun getTeamMembers(teamId: String): Flow<List<TeamMember>> {
        return teamMemberDao.getTeamMembers(teamId).map { members ->
            members.map { it.toDomainModel() }
        }
    }

    override suspend fun createTeam(team: Team): Result<Team> {
        return try {
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.createTeam(team.toEntity().toApiRequest())
                if (response.isSuccessful) {
                    response.body()?.let { teamResponse ->
                        val entity = teamResponse.toEntity()
                        teamDao.insertTeam(entity)
                        Result.success(entity.toDomainModel())
                    } ?: Result.failure(Exception("Empty response body"))
                } else {
                    Result.failure(Exception("Failed to create team: ${response.code()}"))
                }
            } else {
                val entity = team.toEntity()
                teamDao.insertTeam(entity)
                Result.success(entity.toDomainModel())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTeam(team: Team): Result<Team> {
        return try {
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.updateTeam(team.toEntity().toApiRequest())
                if (response.isSuccessful) {
                    response.body()?.let { teamResponse ->
                        val entity = teamResponse.toEntity()
                        teamDao.updateTeam(entity)
                        Result.success(entity.toDomainModel())
                    } ?: Result.failure(Exception("Empty response body"))
                } else {
                    Result.failure(Exception("Failed to update team: ${response.code()}"))
                }
            } else {
                val entity = team.toEntity()
                teamDao.updateTeam(entity)
                Result.success(entity.toDomainModel())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTeam(teamId: String): Result<Unit> {
        return try {
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.deleteTeam(teamId)
                if (response.isSuccessful) {
                    teamDao.deleteTeam(teamId)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to delete team: ${response.code()}"))
                }
            } else {
                teamDao.deleteTeam(teamId)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun inviteUserToTeam(teamId: String, userEmail: String): Result<TeamMember> {
        return try {
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.inviteUserToTeam(teamId, userEmail)
                if (response.isSuccessful) {
                    response.body()?.let { memberResponse ->
                        val entity = memberResponse.toEntity(teamId)
                        teamDao.insertMember(entity)
                        Result.success(entity.toDomainModel())
                    } ?: Result.failure(Exception("Empty response body"))
                } else {
                    Result.failure(Exception("Failed to invite user: ${response.code()}"))
                }
            } else {
                Result.failure(Exception("No network connection"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeUserFromTeam(teamId: String, userId: String): Result<Unit> {
        return try {
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.removeUserFromTeam(teamId, userId)
                if (response.isSuccessful) {
                    teamDao.deleteMember(teamId, userId)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to remove user: ${response.code()}"))
                }
            } else {
                teamDao.deleteMember(teamId, userId)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun changeUserRole(teamId: String, userId: String, newRole: String): Result<TeamMember> {
        return try {
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.changeUserRole(teamId, userId, newRole)
                if (response.isSuccessful) {
                    response.body()?.let { memberResponse ->
                        val entity = memberResponse.toEntity(teamId)
                        teamDao.updateMember(entity)
                        Result.success(entity.toDomainModel())
                    } ?: Result.failure(Exception("Empty response body"))
                } else {
                    Result.failure(Exception("Failed to change user role: ${response.code()}"))
                }
            } else {
                Result.failure(Exception("No network connection"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isUserAdminOfTeam(teamId: String, userId: String): Flow<Boolean> {
        return teamDao.isUserAdminOfTeam(teamId, userId)
    }

    override fun isUserMemberOfTeam(teamId: String, userId: String): Flow<Boolean> {
        return teamDao.isUserMemberOfTeam(teamId, userId)
    }

    override suspend fun syncTeams(): Result<Unit> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                return Result.failure(Exception("No network connection"))
            }

            // Sync pending teams
            val pendingTeams = teamDao.getPendingTeamsSync()
            for (team in pendingTeams) {
                when (team.syncStatus) {
                    "pending_create" -> {
                        val response = apiService.createTeam(team.toApiRequest())
                        if (response.isSuccessful) {
                            response.body()?.let { teamResponse ->
                                val entity = teamResponse.toEntity(team)
                                teamDao.updateTeam(entity)
                            }
                        }
                    }
                    "pending_update" -> {
                        val response = apiService.updateTeam(team.toApiRequest())
                        if (response.isSuccessful) {
                            response.body()?.let { teamResponse ->
                                val entity = teamResponse.toEntity(team)
                                teamDao.updateTeam(entity)
                            }
                        }
                    }
                    "pending_delete" -> {
                        val response = apiService.deleteTeam(team.id)
                        if (response.isSuccessful) {
                            teamDao.deleteTeam(team.id)
                        }
                    }
                }
            }

            // Sync server teams
            val response = apiService.getTeams()
            if (response.isSuccessful) {
                response.body()?.let { teams ->
                    for (teamResponse in teams) {
                        val existingTeam = teamDao.getTeamByServerIdSync(teamResponse.id)
                        val entity = teamResponse.toEntity(existingTeam)
                        if (existingTeam == null) {
                            teamDao.insertTeam(entity)
                        } else {
                            teamDao.updateTeam(entity)
                        }
                    }
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to sync teams: ${response.code()}"))
            }
        } catch (e: Exception) {
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
                            teamMemberDao.deleteMember(member.id)
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