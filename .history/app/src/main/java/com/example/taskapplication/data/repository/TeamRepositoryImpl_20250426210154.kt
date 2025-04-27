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
    private val teamMemberDao: TeamMemberDao,
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager,
    private val connectionChecker: ConnectionChecker
) : TeamRepository {

    private val TAG = "TeamRepository"

    override fun getTeams(): Flow<List<Team>> {
        return teamDao.getAllTeams().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getTeamById(teamId: String): Flow<Team?> {
        return teamDao.getTeamById(teamId).map { it?.toDomainModel() }
    }

    override fun getTeamMembers(teamId: String): Flow<List<TeamMember>> {
        return teamMemberDao.getTeamMembersByTeamId(teamId).map { entities ->
            entities.map { it.toDomainModel() }
        }
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
        val existing = teamDao.getTeamByIdSync(team.id)
            ?: return Result.failure(NoSuchElementException("Team not found"))

        val updatedTeam = team.copy(
            syncStatus = if (existing.serverId != null) "pending_update" else "pending_create",
            lastModified = System.currentTimeMillis()
        ).toEntity()

        teamDao.updateTeam(updatedTeam)

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTeams()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after updating team", e)
            }
        }

        return Result.success(updatedTeam.toDomainModel())
    }

    override suspend fun deleteTeam(teamId: String): Result<Unit> {
        val team = teamDao.getTeamByIdSync(teamId)
            ?: return Result.failure(NoSuchElementException("Team not found"))

        if (team.serverId == null) {
            // If the team has never been synced, just delete it locally
            teamDao.deleteLocalOnlyTeam(teamId)
            teamMemberDao.deleteTeamMembersByTeamId(teamId)
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

    override suspend fun addTeamMember(teamId: String, user: User, role: String): Result<TeamMember> {
        val team = teamDao.getTeamByIdSync(teamId)
            ?: return Result.failure(NoSuchElementException("Team not found"))

        val teamMember = TeamMember(
            id = UUID.randomUUID().toString(),
            teamId = teamId,
            userId = user.id,
            userName = user.name,
            userEmail = user.email,
            role = role,
            syncStatus = "pending_create",
            lastModified = System.currentTimeMillis()
        )

        teamMemberDao.insertTeamMember(teamMember.toEntity())

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTeamMembers()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after adding team member", e)
            }
        }

        return Result.success(teamMember)
    }

    override suspend fun removeTeamMember(teamId: String, userId: String): Result<Unit> {
        val teamMember = teamMemberDao.getTeamMemberByUserIdSync(teamId, userId)
            ?: return Result.failure(NoSuchElementException("Team member not found"))

        if (teamMember.serverId == null) {
            // If the team member has never been synced, just delete it locally
            teamMemberDao.deleteTeamMemberByUserId(teamId, userId)
        } else {
            // Mark for deletion during next sync
            teamMemberDao.markTeamMemberForDeletion(teamMember.id, System.currentTimeMillis())
        }

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTeamMembers()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after removing team member", e)
            }
        }

        return Result.success(Unit)
    }

    override suspend fun updateTeamMemberRole(teamId: String, userId: String, role: String): Result<TeamMember> {
        val teamMember = teamMemberDao.getTeamMemberByUserIdSync(teamId, userId)
            ?: return Result.failure(NoSuchElementException("Team member not found"))

        val updatedTeamMember = teamMember.copy(
            role = role,
            syncStatus = if (teamMember.serverId != null) "pending_update" else "pending_create",
            lastModified = System.currentTimeMillis()
        )

        teamMemberDao.updateTeamMember(updatedTeamMember)

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTeamMembers()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after updating team member role", e)
            }
        }

        return Result.success(updatedTeamMember.toDomainModel())
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
                        teamMemberDao.deleteTeamMembersByTeamId(team.id)
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
                                  localTeam.syncStatus != "pending_delete" &&
                                  localTeam.syncStatus != "pending_update") {
                            // Server has newer version and we're not in the middle of an update or delete
                            teamDao.updateTeam(
                                remoteTeam.toEntity().copy(
                                    id = localTeam.id,
                                    syncStatus = "synced",
                                    serverId = remoteTeam.id
                                )
                            )
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

    override suspend fun syncTeamMembers(): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        try {
            val pendingMembers = teamMemberDao.getPendingSyncTeamMembers()
            
            // Group by sync status
            val membersToCreate = pendingMembers.filter { it.syncStatus == "pending_create" }
            val membersToUpdate = pendingMembers.filter { it.syncStatus == "pending_update" }
            val membersToDelete = pendingMembers.filter { it.syncStatus == "pending_delete" }
            
            // Process creates
            for (member in membersToCreate) {
                try {
                    val response = apiService.addTeamMember(member.teamId, member.toApiRequest())
                    
                    if (response.isSuccessful && response.body() != null) {
                        val serverMember = response.body()!!
                        teamMemberDao.updateTeamMember(
                            member.copy(
                                serverId = serverMember.id,
                                syncStatus = "synced",
                                lastModified = System.currentTimeMillis()
                            )
                        )
                    } else {
                        Log.e(TAG, "Failed to add team member: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error adding team member", e)
                    continue
                }
            }
            
            // Process updates
            for (member in membersToUpdate) {
                if (member.serverId == null) continue
                
                try {
                    val response = apiService.updateTeamMember(member.teamId, member.userId, member.toApiRequest())
                    
                    if (response.isSuccessful) {
                        teamMemberDao.updateTeamMember(
                            member.copy(
                                syncStatus = "synced",
                                lastModified = System.currentTimeMillis()
                            )
                        )
                    } else {
                        Log.e(TAG, "Failed to update team member: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating team member", e)
                    continue
                }
            }
            
            // Process deletes
            for (member in membersToDelete) {
                if (member.serverId == null) continue
                
                try {
                    val response = apiService.removeTeamMember(member.teamId, member.userId)
                    
                    if (response.isSuccessful) {
                        teamMemberDao.deleteTeamMember(member.id)
                    } else {
                        Log.e(TAG, "Failed to remove team member: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing team member", e)
                    continue
                }
            }
            
            // Fetch and merge remote team members
            try {
                val lastSyncTimestamp = dataStoreManager.getLastTeamMemberSyncTimestamp() ?: 0
                val response = apiService.getAllTeamMembers(lastSyncTimestamp)
                
                if (response.isSuccessful && response.body() != null) {
                    val remoteMembers = response.body()!!
                    
                    for (remoteMember in remoteMembers) {
                        val localMember = teamMemberDao.getTeamMemberByServerIdSync(remoteMember.id)
                        
                        if (localMember == null) {
                            // New member from server
                            teamMemberDao.insertTeamMember(
                                remoteMember.toEntity().copy(
                                    id = UUID.randomUUID().toString(),
                                    syncStatus = "synced",
                                    serverId = remoteMember.id
                                )
                            )
                        } else if (remoteMember.lastModified > localMember.lastModified && 
                                  localMember.syncStatus != "pending_delete" &&
                                  localMember.syncStatus != "pending_update") {
                            // Server has newer version and we're not in the middle of an update or delete
                            teamMemberDao.updateTeamMember(
                                remoteMember.toEntity().copy(
                                    id = localMember.id,
                                    syncStatus = "synced",
                                    serverId = remoteMember.id
                                )
                            )
                        }
                    }
                    
                    // Update last sync timestamp
                    dataStoreManager.saveLastTeamMemberSyncTimestamp(System.currentTimeMillis())
                } else {
                    Log.e(TAG, "Failed to fetch remote team members: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching remote team members", e)
            }
            
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error during team member sync", e)
            return Result.failure(e)
        }
    }
} 