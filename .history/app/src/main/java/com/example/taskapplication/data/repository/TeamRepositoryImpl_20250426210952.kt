package com.example.taskapplication.data.repository

import android.util.Log
import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.database.dao.TeamDao
import com.example.taskapplication.data.database.dao.TeamMemberDao
import com.example.taskapplication.data.mapper.toApiRequest
import com.example.taskapplication.data.mapper.toDomainModel
import com.example.taskapplication.data.mapper.toEntity
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.domain.model.Team
import com.example.taskapplication.domain.model.TeamMember
import com.example.taskapplication.domain.model.User
import com.example.taskapplication.domain.repository.TeamRepository
import com.example.taskapplication.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeamRepositoryImpl @Inject constructor(
    private val teamDao: TeamDao,
    private val teamMemberDao: TeamMemberDao,
    private val apiService: ApiService,
    private val userRepository: UserRepository,
    private val dataStoreManager: DataStoreManager,
    private val connectionChecker: ConnectionChecker
) : TeamRepository {

    private val TAG = "TeamRepository"

    override fun getAllTeams(): Flow<List<Team>> {
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
        val currentUserId = dataStoreManager.getCurrentUserId() ?: 
            return Result.failure(IllegalStateException("No current user found"))
        
        val teamWithId = team.copy(
            id = UUID.randomUUID().toString(),
            createdBy = currentUserId,
            createdAt = System.currentTimeMillis(),
            syncStatus = "pending_create",
            lastModified = System.currentTimeMillis()
        )
        
        teamDao.insertTeam(teamWithId.toEntity())
        
        // Add current user as team member and admin
        val teamMember = TeamMember(
            id = UUID.randomUUID().toString(), 
            teamId = teamWithId.id,
            userId = currentUserId,
            role = "admin",
            joinedAt = System.currentTimeMillis(),
            syncStatus = "pending_create",
            lastModified = System.currentTimeMillis()
        )
        
        teamMemberDao.insertTeamMember(teamMember.toEntity())
        
        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTeams()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after creating team", e)
            }
        }
        
        return Result.success(teamWithId)
    }

    override suspend fun updateTeam(team: Team): Result<Team> {
        val existingTeam = teamDao.getTeamByIdSync(team.id)
            ?: return Result.failure(NoSuchElementException("Team not found"))
        
        val currentUserId = dataStoreManager.getCurrentUserId() ?: 
            return Result.failure(IllegalStateException("No current user found"))
        
        // Check if user is admin of team
        val isAdmin = teamMemberDao.isUserAdminOfTeam(team.id, currentUserId)
        if (!isAdmin) {
            return Result.failure(IllegalStateException("Only team admin can update team"))
        }
        
        val updatedTeam = team.copy(
            syncStatus = if (existingTeam.serverId == null) "pending_create" else "pending_update",
            lastModified = System.currentTimeMillis()
        )
        
        teamDao.updateTeam(updatedTeam.toEntity())
        
        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTeams()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after updating team", e)
            }
        }
        
        return Result.success(updatedTeam)
    }

    override suspend fun deleteTeam(teamId: String): Result<Unit> {
        val team = teamDao.getTeamByIdSync(teamId)
            ?: return Result.failure(NoSuchElementException("Team not found"))
        
        val currentUserId = dataStoreManager.getCurrentUserId() ?: 
            return Result.failure(IllegalStateException("No current user found"))
        
        // Check if user is admin of team
        val isAdmin = teamMemberDao.isUserAdminOfTeam(teamId, currentUserId)
        if (!isAdmin) {
            return Result.failure(IllegalStateException("Only team admin can delete team"))
        }
        
        if (team.serverId == null) {
            // If team has never been synced, just delete locally
            teamDao.deleteTeam(teamId)
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

    override suspend fun inviteTeamMember(teamId: String, email: String): Result<Unit> {
        val team = teamDao.getTeamByIdSync(teamId)
            ?: return Result.failure(NoSuchElementException("Team not found"))
        
        val currentUserId = dataStoreManager.getCurrentUserId() ?: 
            return Result.failure(IllegalStateException("No current user found"))
        
        // Check if user is admin of team
        val isAdmin = teamMemberDao.isUserAdminOfTeam(teamId, currentUserId)
        if (!isAdmin) {
            return Result.failure(IllegalStateException("Only team admin can invite members"))
        }
        
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }
        
        try {
            val response = apiService.inviteTeamMember(teamId, email)
            
            return if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("Failed to invite team member: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inviting team member", e)
            return Result.failure(e)
        }
    }

    override suspend fun removeMember(teamId: String, userId: String): Result<Unit> {
        val team = teamDao.getTeamByIdSync(teamId)
            ?: return Result.failure(NoSuchElementException("Team not found"))
        
        val currentUserId = dataStoreManager.getCurrentUserId() ?: 
            return Result.failure(IllegalStateException("No current user found"))
        
        // Check if current user is admin or if they're removing themselves
        val isAdmin = teamMemberDao.isUserAdminOfTeam(teamId, currentUserId)
        if (!isAdmin && currentUserId != userId) {
            return Result.failure(IllegalStateException("Only team admin can remove other members"))
        }
        
        val teamMember = teamMemberDao.getTeamMemberSync(teamId, userId)
            ?: return Result.failure(NoSuchElementException("Team member not found"))
        
        if (teamMember.serverId == null) {
            // If member has never been synced, just delete locally
            teamMemberDao.deleteTeamMember(teamId, userId)
        } else {
            // Mark for deletion during next sync
            teamMemberDao.markTeamMemberForDeletion(teamId, userId, System.currentTimeMillis())
        }
        
        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTeams()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after removing team member", e)
            }
        }
        
        return Result.success(Unit)
    }

    override suspend fun updateMemberRole(teamId: String, userId: String, newRole: String): Result<Unit> {
        val team = teamDao.getTeamByIdSync(teamId)
            ?: return Result.failure(NoSuchElementException("Team not found"))
        
        val currentUserId = dataStoreManager.getCurrentUserId() ?: 
            return Result.failure(IllegalStateException("No current user found"))
        
        // Check if user is admin of team
        val isAdmin = teamMemberDao.isUserAdminOfTeam(teamId, currentUserId)
        if (!isAdmin) {
            return Result.failure(IllegalStateException("Only team admin can update member roles"))
        }
        
        val teamMember = teamMemberDao.getTeamMemberSync(teamId, userId)
            ?: return Result.failure(NoSuchElementException("Team member not found"))
        
        // Validate role
        if (newRole != "admin" && newRole != "member") {
            return Result.failure(IllegalArgumentException("Invalid role. Must be 'admin' or 'member'"))
        }
        
        teamMemberDao.updateTeamMemberRole(
            teamId, 
            userId, 
            newRole, 
            if (teamMember.serverId == null) "pending_create" else "pending_update",
            System.currentTimeMillis()
        )
        
        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTeams()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after updating member role", e)
            }
        }
        
        return Result.success(Unit)
    }

    override suspend fun joinTeam(inviteCode: String): Result<Team> {
        val currentUserId = dataStoreManager.getCurrentUserId() ?: 
            return Result.failure(IllegalStateException("No current user found"))
        
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }
        
        try {
            val response = apiService.joinTeam(inviteCode)
            
            if (response.isSuccessful && response.body() != null) {
                val team = response.body()!!
                teamDao.insertTeam(team.toEntity())
                
                val teamMember = TeamMember(
                    id = UUID.randomUUID().toString(),
                    teamId = team.id,
                    userId = currentUserId,
                    role = "member",
                    joinedAt = System.currentTimeMillis(),
                    serverId = team.id, // Use team id as server id for now
                    syncStatus = "synced",
                    lastModified = System.currentTimeMillis()
                )
                
                teamMemberDao.insertTeamMember(teamMember.toEntity())
                
                return Result.success(team.toDomainModel())
            } else {
                return Result.failure(IOException("Failed to join team: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error joining team", e)
            return Result.failure(e)
        }
    }

    override suspend fun syncTeams(): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }
        
        try {
            // Sync teams
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
                try {
                    val response = apiService.updateTeam(team.serverId!!, team.toApiRequest())
                    
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
                if (team.serverId == null) {
                    // If team was never synced, we can just delete it locally
                    teamDao.deleteTeam(team.id)
                    continue
                }
                
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
            
            // Sync team members
            val pendingMembers = teamMemberDao.getPendingSyncTeamMembers()
            
            // Group by sync status
            val membersToCreate = pendingMembers.filter { it.syncStatus == "pending_create" }
            val membersToUpdate = pendingMembers.filter { it.syncStatus == "pending_update" }
            val membersToDelete = pendingMembers.filter { it.syncStatus == "pending_delete" }
            
            // Process creates
            for (member in membersToCreate) {
                try {
                    val team = teamDao.getTeamByIdSync(member.teamId)
                        ?: continue // Skip if team not found
                    
                    if (team.serverId == null) {
                        continue // Skip if team is not synced yet
                    }
                    
                    val response = apiService.addTeamMember(team.serverId, member.toApiRequest())
                    
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
                        Log.e(TAG, "Failed to create team member: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating team member", e)
                    continue
                }
            }
            
            // Process updates
            for (member in membersToUpdate) {
                try {
                    val team = teamDao.getTeamByIdSync(member.teamId)
                        ?: continue // Skip if team not found
                    
                    if (team.serverId == null || member.serverId == null) {
                        continue // Skip if team or member is not synced yet
                    }
                    
                    val response = apiService.updateTeamMember(team.serverId, member.serverId, member.toApiRequest())
                    
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
                if (member.serverId == null) {
                    // If member was never synced, we can just delete it locally
                    teamMemberDao.deleteTeamMember(member.teamId, member.userId)
                    continue
                }
                
                try {
                    val team = teamDao.getTeamByIdSync(member.teamId)
                        ?: continue // Skip if team not found
                    
                    if (team.serverId == null) {
                        continue // Skip if team is not synced yet
                    }
                    
                    val response = apiService.removeTeamMember(team.serverId, member.serverId)
                    
                    if (response.isSuccessful) {
                        teamMemberDao.deleteTeamMember(member.teamId, member.userId)
                    } else {
                        Log.e(TAG, "Failed to delete team member: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting team member", e)
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
            
            // Fetch and merge remote team members
            try {
                val lastSyncTimestamp = dataStoreManager.getLastTeamMemberSyncTimestamp() ?: 0
                val response = apiService.getTeamMembers(lastSyncTimestamp)
                
                if (response.isSuccessful && response.body() != null) {
                    val remoteMembers = response.body()!!
                    
                    for (remoteMember in remoteMembers) {
                        val localTeam = teamDao.getTeamByServerIdSync(remoteMember.teamId)
                            ?: continue // Skip if team doesn't exist locally
                            
                        val localMember = teamMemberDao.getTeamMemberByServerIdSync(remoteMember.id)
                        
                        if (localMember == null) {
                            // New member from server
                            teamMemberDao.insertTeamMember(
                                remoteMember.toEntity().copy(
                                    id = UUID.randomUUID().toString(),
                                    teamId = localTeam.id, // Use local team ID
                                    syncStatus = "synced",
                                    serverId = remoteMember.id
                                )
                            )
                        } else if (remoteMember.lastModified > localMember.lastModified && 
                                  localMember.syncStatus != "pending_update" && 
                                  localMember.syncStatus != "pending_delete") {
                            // Server has newer version and we're not in the middle of an update or delete
                            teamMemberDao.updateTeamMember(
                                remoteMember.toEntity().copy(
                                    id = localMember.id,
                                    teamId = localTeam.id, // Use local team ID
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
            Log.e(TAG, "Error during team sync", e)
            return Result.failure(e)
        }
    }
} 