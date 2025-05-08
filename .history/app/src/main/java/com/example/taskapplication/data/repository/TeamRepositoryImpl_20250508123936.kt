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

    override suspend fun getTeamById(id: String): Result<Team> {
        return try {
            val entity = teamDao.getTeamById(id)
            if (entity != null) {
                Result.success(entity.toDomainModel())
            } else {
                Result.failure(IllegalStateException("Team not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getTeamMembers(teamId: String): Flow<List<TeamMember>> {
        return teamMemberDao.getTeamMembers(teamId).map { members ->
            members.map { it.toDomainModel() }
        }
    }

    override suspend fun createTeam(team: Team): Result<Team> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                val entity = team.toEntity()
                teamDao.insertTeam(entity)
                Result.success(team)
            } else {
                val request = team.toEntity().toApiRequest()
                val response = apiService.createTeam(request)
                val entity = response.toEntity()
                teamDao.insertTeam(entity)
                Result.success(entity.toDomainModel())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTeam(team: Team): Result<Team> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                val entity = team.toEntity()
                teamDao.updateTeam(entity)
                Result.success(team)
            } else {
                val request = team.toEntity().toApiRequest()
                val response = apiService.updateTeam(team.id, request)
                val entity = response.toEntity()
                teamDao.updateTeam(entity)
                Result.success(entity.toDomainModel())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTeam(teamId: String): Result<Unit> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                teamDao.deleteTeam(teamId)
                Result.success(Unit)
            } else {
                apiService.deleteTeam(teamId)
                teamDao.deleteTeam(teamId)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun inviteUserToTeam(teamId: String, userEmail: String): Result<Unit> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                Result.failure(IOException("No network connection"))
            } else {
                apiService.inviteUserToTeam(teamId, userEmail)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeUserFromTeam(teamId: String, userId: String): Result<Unit> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                teamMemberDao.deleteMember(teamId, userId)
                Result.success(Unit)
            } else {
                apiService.removeUserFromTeam(teamId, userId)
                teamMemberDao.deleteMember(teamId, userId)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun changeUserRole(teamId: String, userId: String, newRole: String): Result<Unit> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                teamMemberDao.updateMemberRole(teamId, userId, newRole)
                Result.success(Unit)
            } else {
                apiService.changeUserRole(teamId, userId, newRole)
                teamMemberDao.updateMemberRole(teamId, userId, newRole)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isUserAdminOfTeam(teamId: String, userId: String): Boolean {
        return teamMemberDao.isUserAdminOfTeam(teamId, userId)
    }

    override suspend fun isUserMemberOfTeam(teamId: String, userId: String): Boolean {
        return teamMemberDao.isUserMemberOfTeam(teamId, userId)
    }

    override suspend fun syncTeams(): Result<Unit> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                Result.failure(IOException("No network connection"))
            } else {
                val pendingTeams = teamDao.getPendingTeams()
                for (team in pendingTeams) {
                    when (team.syncStatus) {
                        "pending_create" -> {
                            val request = team.toApiRequest()
                            val response = apiService.createTeam(request)
                            val entity = response.toEntity()
                            teamDao.insertTeam(entity)
                        }
                        "pending_update" -> {
                            val request = team.toApiRequest()
                            val response = apiService.updateTeam(team.id, request)
                            val entity = response.toEntity()
                            teamDao.updateTeam(entity)
                        }
                        "pending_delete" -> {
                            apiService.deleteTeam(team.id)
                            teamDao.deleteTeam(team.id)
                        }
                    }
                }

                val serverTeams = apiService.getTeams()
                for (teamResponse in serverTeams) {
                    val entity = teamResponse.toEntity()
                    teamDao.insertTeam(entity)
                }

                Result.success(Unit)
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