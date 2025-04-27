package com.example.taskapplication.data.repository

import android.util.Log
import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.database.dao.TeamDao
import com.example.taskapplication.data.database.dao.TeamMemberDao
import com.example.taskapplication.data.mapper.toDomainModel
import com.example.taskapplication.data.mapper.toEntity
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.domain.model.Team
import com.example.taskapplication.domain.model.TeamMember
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
    private val teamMemberDao: TeamMemberDao,
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager,
    private val connectionChecker: ConnectionChecker
) : TeamRepository {

    private val TAG = "TeamRepository"

    override fun getUserTeams(): Flow<List<Team>> {
        return teamDao.getAllTeams().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getTeam(teamId: String): Flow<Team?> {
        return teamDao.getTeamById(teamId).map { it?.toDomainModel() }
    }

    override suspend fun createTeam(team: Team): Result<Team> {
        val teamEntity = team.copy(
            id = UUID.randomUUID().toString(),
            syncStatus = "pending_create",
            lastModified = System.currentTimeMillis()
        ).toEntity()

        teamDao.insertTeam(teamEntity)

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTeams()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after creating team", e)
            }
        }

        return Result.success(teamEntity.toDomainModel())
    }

    override suspend fun updateTeam(team: Team): Result<Team> {
        val existingTeam = teamDao.getTeamByIdSync(team.id)
            ?: return Result.failure(NoSuchElementException("Team not found"))

        val updatedEntity = team.copy(
            syncStatus = if (existingTeam.serverId != null) "pending_update" else "pending_create",
            lastModified = System.currentTimeMillis(),
            serverId = existingTeam.serverId
        ).toEntity()

        teamDao.updateTeam(updatedEntity)

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTeams()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after updating team", e)
            }
        }

        return Result.success(updatedEntity.toDomainModel())
    }

    override suspend fun deleteTeam(teamId: String): Result<Unit> {
        val team = teamDao.getTeamByIdSync(teamId)
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
                Log.e(TAG, "Error syncing after deleting team", e)
            }
        }

        return Result.success(Unit)
    }

    override fun getTeamMembers(teamId: String): Flow<List<TeamMember>> {
        return teamMemberDao.getTeamMembers(teamId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun addTeamMember(teamId: String, email: String): Result<TeamMember> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        try {
            val response = apiService.inviteTeamMember(teamId, mapOf("email" to email))
            if (response.isSuccessful && response.body() != null) {
                val memberResponse = response.body()!!
                val teamMember = TeamMember(
                    id = UUID.randomUUID().toString(),
                    teamId = teamId,
                    userId = memberResponse.userId,
                    email = email,
                    name = memberResponse.name,
                    role = memberResponse.role,
                    status = memberResponse.status
                )
                
                teamMemberDao.insertTeamMember(teamMember.toEntity())
                return Result.success(teamMember)
            } else {
                return Result.failure(IOException("Failed to add team member: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding team member", e)
            return Result.failure(e)
        }
    }

    override suspend fun removeTeamMember(teamId: String, memberId: String): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        try {
            val response = apiService.removeTeamMember(teamId, memberId)
            if (response.isSuccessful) {
                teamMemberDao.deleteTeamMember(memberId)
                return Result.success(Unit)
            } else {
                return Result.failure(IOException("Failed to remove team member: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing team member", e)
            return Result.failure(e)
        }
    }

    override suspend fun updateTeamMemberRole(teamId: String, memberId: String, role: String): Result<TeamMember> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        try {
            val response = apiService.updateTeamMemberRole(teamId, memberId, mapOf("role" to role))
            if (response.isSuccessful && response.body() != null) {
                val memberResponse = response.body()!!
                val teamMember = teamMemberDao.getTeamMemberByIdSync(memberId)?.toDomainModel()
                    ?: return Result.failure(NoSuchElementException("Team member not found"))
                
                val updatedMember = teamMember.copy(role = role)
                teamMemberDao.updateTeamMember(updatedMember.toEntity())
                
                return Result.success(updatedMember)
            } else {
                return Result.failure(IOException("Failed to update team member role: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating team member role", e)
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
                        teamDao.updateTeam(
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
                        teamDao.updateTeam(
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
            
            // Fetch and merge remote teams
            try {
                val lastSyncTimestamp = dataStoreManager.getLastTeamSyncTimestamp() ?: 0
                val response = apiService.getTeams(lastSyncTimestamp)
                
                if (response.isSuccessful && response.body() != null) {
                    val remoteTeams = response.body()!!
                    
                    for (remoteTeam in remoteTeams) {
                        val localTeam = teamDao.getTeamByServerIdSync(remoteTeam.id)
                        
                        if (localTeam == null) {
                            // New team from server
                            teamDao.insertTeam(
                                remoteTeam.toEntity().copy(
                                    id = UUID.randomUUID().toString(),
                                    syncStatus = "synced",
                                    serverId = remoteTeam.id
                                )
                            )
                        } else if (remoteTeam.lastModified > localTeam.lastModified && 
                                   localTeam.syncStatus != "pending_update" && 
                                   localTeam.syncStatus != "pending_delete") {
                            // Server has newer version and we're not in the middle of an update
                            teamDao.updateTeam(
                                remoteTeam.toEntity().copy(
                                    id = localTeam.id,
                                    syncStatus = "synced",
                                    serverId = remoteTeam.id
                                )
                            )
                        }
                    }
                    
                    // Fetch team members for all teams
                    for (team in teamDao.getAllTeamsSync()) {
                        if (team.serverId == null) continue
                        
                        val membersResponse = apiService.getTeamMembers(team.serverId)
                        if (membersResponse.isSuccessful && membersResponse.body() != null) {
                            val remoteMembers = membersResponse.body()!!
                            
                            // Clear existing members and replace with server data
                            teamMemberDao.deleteTeamMembers(team.id)
                            
                            for (remoteMember in remoteMembers) {
                                teamMemberDao.insertTeamMember(
                                    remoteMember.toEntity().copy(
                                        id = UUID.randomUUID().toString(),
                                        teamId = team.id
                                    )
                                )
                            }
                        }
                    }
                    
                    // Update last sync timestamp
                    dataStoreManager.saveLastTeamSyncTimestamp(System.currentTimeMillis())
                } else {
                    Log.e(TAG, "Failed to fetch remote teams: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching remote teams", e)
            }
            
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error during team sync", e)
            return Result.failure(e)
        }
    }
} 