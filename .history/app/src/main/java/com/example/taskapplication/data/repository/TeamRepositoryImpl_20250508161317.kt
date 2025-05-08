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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emit
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
        return teamDao.getAllTeams()
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                // Log error and emit empty list
                emit(emptyList())
            }
    }

    override fun getTeamsForUser(userId: String): Flow<List<Team>> {
        return teamDao.getTeamsForUser(userId)
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                // Log error and emit empty list
                emit(emptyList())
            }
    }

    override fun getTeamById(teamId: String): Flow<Team?> {
        return teamDao.getTeamById(teamId)
            .map { entity -> entity?.toDomainModel() }
            .catch { e ->
                // Log error and emit null
                emit(null)
            }
    }

    override fun getTeamMembers(teamId: String): Flow<List<TeamMember>> {
        return teamMemberDao.getTeamMembers(teamId)
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                // Log error and emit empty list
                emit(emptyList())
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
                    } ?: Result.failure(IOException("Empty response from server"))
                } else {
                    // If server fails, save locally with pending status
                    val entity = team.toEntity().copy(syncStatus = "pending_create")
                    teamDao.insertTeam(entity)
                    Result.success(entity.toDomainModel())
                }
            } else {
                // Save locally with pending status
                val entity = team.toEntity().copy(syncStatus = "pending_create")
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
                    } ?: Result.failure(IOException("Empty response from server"))
                } else {
                    // If server fails, update locally with pending status
                    val entity = team.toEntity().copy(syncStatus = "pending_update")
                    teamDao.updateTeam(entity)
                    Result.success(entity.toDomainModel())
                }
            } else {
                // Update locally with pending status
                val entity = team.toEntity().copy(syncStatus = "pending_update")
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
                    // If server fails, mark for deletion locally
                    teamDao.markTeamForDeletion(teamId, System.currentTimeMillis())
                    Result.success(Unit)
                }
            } else {
                // Mark for deletion locally
                teamDao.markTeamForDeletion(teamId, System.currentTimeMillis())
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addTeamMember(teamId: String, userId: String, role: String): Result<TeamMember> {
        return try {
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.addTeamMember(teamId, userId, role)
                if (response.isSuccessful) {
                    response.body()?.let { memberResponse ->
                        val entity = memberResponse.toEntity()
                        teamMemberDao.insertTeamMember(entity)
                        Result.success(entity.toDomainModel())
                    } ?: Result.failure(IOException("Empty response from server"))
                } else {
                    // If server fails, save locally with pending status
                    val entity = TeamMember(teamId, userId, role, System.currentTimeMillis(), "pending_create")
                    teamMemberDao.insertTeamMember(entity)
                    Result.success(entity.toDomainModel())
                }
            } else {
                // Save locally with pending status
                val entity = TeamMember(teamId, userId, role, System.currentTimeMillis(), "pending_create")
                teamMemberDao.insertTeamMember(entity)
                Result.success(entity.toDomainModel())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeTeamMember(teamId: String, userId: String): Result<Unit> {
        return try {
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.removeTeamMember(teamId, userId)
                if (response.isSuccessful) {
                    teamMemberDao.deleteTeamMember(teamId, userId)
                    Result.success(Unit)
                } else {
                    // If server fails, mark for deletion locally
                    teamMemberDao.markTeamMemberForDeletion(teamId, userId, System.currentTimeMillis())
                    Result.success(Unit)
                }
            } else {
                // Mark for deletion locally
                teamMemberDao.markTeamMemberForDeletion(teamId, userId, System.currentTimeMillis())
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTeamMemberRole(teamId: String, userId: String, role: String): Result<TeamMember> {
        return try {
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.updateTeamMemberRole(teamId, userId, role)
                if (response.isSuccessful) {
                    response.body()?.let { memberResponse ->
                        val entity = memberResponse.toEntity()
                        teamMemberDao.updateTeamMember(entity)
                        Result.success(entity.toDomainModel())
                    } ?: Result.failure(IOException("Empty response from server"))
                } else {
                    // If server fails, update locally with pending status
                    val entity = TeamMember(teamId, userId, role, System.currentTimeMillis(), "pending_update")
                    teamMemberDao.updateTeamMember(entity)
                    Result.success(entity.toDomainModel())
                }
            } else {
                // Update locally with pending status
                val entity = TeamMember(teamId, userId, role, System.currentTimeMillis(), "pending_update")
                teamMemberDao.updateTeamMember(entity)
                Result.success(entity.toDomainModel())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncTeams(): Result<Unit> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                return Result.failure(IOException("No network connection"))
            }

            // Get pending teams
            val pendingTeams = teamDao.getPendingSyncTeams()

            // Sync each pending team
            for (team in pendingTeams) {
                when (team.syncStatus) {
                    "pending_create" -> {
                        val response = apiService.createTeam(team.toApiRequest())
                        if (response.isSuccessful) {
                            response.body()?.let { teamResponse ->
                                val entity = teamResponse.toEntity()
                                teamDao.updateTeam(entity)
                            }
                        }
                    }
                    "pending_update" -> {
                        val response = apiService.updateTeam(team.toApiRequest())
                        if (response.isSuccessful) {
                            response.body()?.let { teamResponse ->
                                val entity = teamResponse.toEntity()
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

            // Get pending team members
            val pendingTeamMembers = teamMemberDao.getPendingSyncTeamMembers()

            // Sync each pending team member
            for (member in pendingTeamMembers) {
                when (member.syncStatus) {
                    "pending_create" -> {
                        val response = apiService.addTeamMember(member.teamId, member.userId, member.role)
                        if (response.isSuccessful) {
                            response.body()?.let { memberResponse ->
                                val entity = memberResponse.toEntity()
                                teamMemberDao.updateTeamMember(entity)
                            }
                        }
                    }
                    "pending_update" -> {
                        val response = apiService.updateTeamMemberRole(member.teamId, member.userId, member.role)
                        if (response.isSuccessful) {
                            response.body()?.let { memberResponse ->
                                val entity = memberResponse.toEntity()
                                teamMemberDao.updateTeamMember(entity)
                            }
                        }
                    }
                    "pending_delete" -> {
                        val response = apiService.removeTeamMember(member.teamId, member.userId)
                        if (response.isSuccessful) {
                            teamMemberDao.deleteTeamMember(member.teamId, member.userId)
                        }
                    }
                }
            }

            Result.success(Unit)
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