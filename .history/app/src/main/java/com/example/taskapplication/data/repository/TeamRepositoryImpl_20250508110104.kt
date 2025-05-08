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

    override suspend fun getTeams(): Flow<List<Team>> = flow {
        val teams = teamDao.getAllTeams().map { it.toDomainModel() }
        emit(teams)
    }

    override suspend fun getTeamsSync(): List<Team> = withContext(Dispatchers.IO) {
        teamDao.getAllTeamsSync().map { it.toDomainModel() }
    }

    override suspend fun getTeamById(id: String): Team? = withContext(Dispatchers.IO) {
        teamDao.getTeamById(id.toLong())?.toDomainModel()
    }

    override suspend fun getTeamByIdSync(id: String): Team? = withContext(Dispatchers.IO) {
        teamDao.getTeamByIdSync(id.toLong())?.toDomainModel()
    }

    override suspend fun getTeamByServerId(serverId: String): Team? = withContext(Dispatchers.IO) {
        teamDao.getTeamByServerId(serverId)?.toDomainModel()
    }

    override suspend fun getTeamByServerIdSync(serverId: String): Team? = withContext(Dispatchers.IO) {
        teamDao.getTeamByServerIdSync(serverId)?.toDomainModel()
    }

    override suspend fun createTeam(team: Team): Result<Team> = withContext(Dispatchers.IO) {
        try {
            val entity = team.toEntity()
            entity.syncStatus = if (networkUtils.isNetworkAvailable()) "synced" else "pending"
            val id = teamDao.insertTeam(entity)
            val createdTeam = teamDao.getTeamById(id)?.toDomainModel()
                ?: return@withContext Result.failure(Exception("Failed to create team"))
            
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.createTeam(entity.toApiRequest())
                val updatedEntity = response.toEntity(entity)
                teamDao.updateTeam(updatedEntity)
                Result.success(updatedEntity.toDomainModel())
            } else {
                Result.success(createdTeam)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTeam(team: Team): Result<Team> = withContext(Dispatchers.IO) {
        try {
            val entity = team.toEntity()
            entity.syncStatus = if (networkUtils.isNetworkAvailable()) "synced" else "pending"
            teamDao.updateTeam(entity)
            val updatedTeam = teamDao.getTeamById(entity.id)?.toDomainModel()
                ?: return@withContext Result.failure(Exception("Failed to update team"))
            
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.updateTeam(entity.id, entity.toApiRequest())
                val updatedEntity = response.toEntity(entity)
                teamDao.updateTeam(updatedEntity)
                Result.success(updatedEntity.toDomainModel())
            } else {
                Result.success(updatedTeam)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTeam(team: Team): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = team.toEntity()
            if (networkUtils.isNetworkAvailable()) {
                apiService.deleteTeam(entity.id)
                teamDao.deleteTeam(entity)
                Result.success(Unit)
            } else {
                entity.syncStatus = "pending"
                teamDao.updateTeam(entity)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun inviteUserToTeam(teamId: String, userEmail: String): Result<TeamMember> = withContext(Dispatchers.IO) {
        try {
            val team = teamDao.getTeamById(teamId.toLong())
                ?: return@withContext Result.failure(Exception("Team not found"))
            
            if (!networkUtils.isNetworkAvailable()) {
                return@withContext Result.failure(Exception("No network connection"))
            }

            val response = apiService.inviteUserToTeam(teamId, userEmail)
            val member = response.toEntity(teamId = teamId, joinedAt = System.currentTimeMillis())
            teamDao.insertMember(member)
            Result.success(member.toDomainModel())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeUserFromTeam(teamId: String, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val team = teamDao.getTeamById(teamId.toLong())
                ?: return@withContext Result.failure(Exception("Team not found"))
            
            if (!networkUtils.isNetworkAvailable()) {
                return@withContext Result.failure(Exception("No network connection"))
            }

            apiService.removeUserFromTeam(teamId, userId)
            teamDao.deleteMember(teamId.toLong(), userId.toLong())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncTeams(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!networkUtils.isNetworkAvailable()) {
                return@withContext Result.failure(Exception("No network connection"))
            }

            val pendingTeams = teamDao.getPendingTeamsSync()
            for (team in pendingTeams) {
                when (team.syncStatus) {
                    "pending" -> {
                        val response = apiService.createTeam(team.toApiRequest())
                        val updatedEntity = response.toEntity(team)
                        teamDao.updateTeam(updatedEntity)
                    }
                    "deleted" -> {
                        apiService.deleteTeam(team.id)
                        teamDao.deleteTeam(team)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncTeamsByUser(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!networkUtils.isNetworkAvailable()) {
                return@withContext Result.failure(Exception("No network connection"))
            }

            val pendingTeams = teamDao.getPendingTeamsByUserSync(userId)
            for (team in pendingTeams) {
                when (team.syncStatus) {
                    "pending" -> {
                        val response = apiService.createTeam(team.toApiRequest())
                        val updatedEntity = response.toEntity(team)
                        teamDao.updateTeam(updatedEntity)
                    }
                    "deleted" -> {
                        apiService.deleteTeam(team.id)
                        teamDao.deleteTeam(team)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncTeamMembers(teamId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!networkUtils.isNetworkAvailable()) {
                return@withContext Result.failure(Exception("No network connection"))
            }

            val pendingMembers = teamDao.getPendingMembersSync(teamId)
            for (member in pendingMembers) {
                when (member.syncStatus) {
                    "pending" -> {
                        val response = apiService.createTeamMember(member.toApiRequest())
                        val updatedEntity = response.toEntity(member)
                        teamDao.updateMember(updatedEntity)
                    }
                    "deleted" -> {
                        apiService.deleteTeamMember(member.id)
                        teamDao.deleteMember(member)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}