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
    private val teamDao: TeamDao,
    private val teamMemberDao: TeamMemberDao,
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

    override fun getTeamsForUser(userId: String): Flow<List<Team>> {
        return teamDao.getTeamsForUser(userId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getTeamById(teamId: String): Flow<Team?> {
        return teamDao.getTeamById(teamId).map { it?.toDomainModel() }
    }

    override suspend fun createTeam(team: Team): Result<Team> {
        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        val teamId = UUID.randomUUID().toString()
        val newTeam = team.copy(
            id = teamId,
            createdBy = currentUserId,
            syncStatus = "pending_create",
            lastModified = System.currentTimeMillis()
        )

        teamDao.insertTeam(newTeam.toEntity())

        // Add the creator as admin member
        val adminMember = TeamMember(
            id = UUID.randomUUID().toString(),
            teamId = teamId,
            userId = currentUserId,
            role = "admin",
            joinedAt = System.currentTimeMillis(),
            invitedBy = currentUserId,
            syncStatus = "pending_create",
            lastModified = System.currentTimeMillis()
        )

        teamMemberDao.insertTeamMember(adminMember.toEntity())

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTeams()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after creating team", e)
            }
        }

        return Result.success(newTeam)
    }

    override suspend fun updateTeam(team: Team): Result<Team> {
        val existingTeam = teamDao.getTeamByIdSync(team.id)
            ?: return Result.failure(NoSuchElementException("Team not found"))

        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        // Check if user is admin of the team
        val isAdmin = teamMemberDao.isUserAdminOfTeam(team.id, currentUserId)
        if (!isAdmin) {
            return Result.failure(IllegalStateException("Only team admins can update the team"))
        }

        val updatedTeam = existingTeam.copy(
            name = team.name,
            description = team.description,
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

        return Result.success(updatedTeam.toDomainModel())
    }

    override suspend fun deleteTeam(teamId: String): Result<Unit> {
        val team = teamDao.getTeamByIdSync(teamId)
            ?: return Result.failure(NoSuchElementException("Team not found"))

        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        // Check if user is admin of the team
        val isAdmin = teamMemberDao.isUserAdminOfTeam(teamId, currentUserId)
        if (!isAdmin) {
            return Result.failure(IllegalStateException("Only team admins can delete the team"))
        }

        if (team.serverId == null) {
            // If team has never been synced, just delete locally
            teamDao.deleteTeam(teamId)
            teamMemberDao.deleteTeamMembers(teamId)
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

    override suspend fun inviteUserToTeam(teamId: String, userEmail: String): Result<TeamMember> {
        val team = teamDao.getTeamByIdSync(teamId)
            ?: return Result.failure(NoSuchElementException("Team not found"))

        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        // Check if user is admin of the team
        val isAdmin = teamMemberDao.isUserAdminOfTeam(teamId, currentUserId)
        if (!isAdmin) {
            return Result.failure(IllegalStateException("Only team admins can invite users"))
        }

        // Try to find user by email from server
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        try {
            val request = AddTeamMemberRequest(
                user_id = userEmail,
                role = "member"
            )

            val response = apiService.addTeamMember(teamId.toLong(), request)

            if (!response.isSuccessful) {
                return Result.failure(Exception("Failed to add team member: ${response.errorBody()?.string()}"))
            }

            // Create the member
            val member = TeamMember(
                id = UUID.randomUUID().toString(),
                teamId = teamId,
                userId = userEmail,
                role = "member",
                joinedAt = System.currentTimeMillis(),
                invitedBy = currentUserId,
                syncStatus = "pending_create",
                lastModified = System.currentTimeMillis()
            )

            teamMemberDao.insertTeamMember(member.toEntity())

            // If connected, try to sync immediately
            if (connectionChecker.isNetworkAvailable()) {
                try {
                    syncTeamMembers()
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing after inviting user", e)
                }
            }

            return Result.success(member)
        } catch (e: Exception) {
            Log.e(TAG, "Error inviting user", e)
            return Result.failure(e)
        }
    }

    override suspend fun removeUserFromTeam(teamId: String, userId: String): Result<Unit> {
        val team = teamDao.getTeamByIdSync(teamId)
            ?: return Result.failure(NoSuchElementException("Team not found"))

        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        // User can remove themselves, or admins can remove others
        if (userId != currentUserId) {
            val isAdmin = teamMemberDao.isUserAdminOfTeam(teamId, currentUserId)
            if (!isAdmin) {
                return Result.failure(IllegalStateException("Only team admins can remove other users"))
            }
        }

        // Cannot remove the last admin
        if (teamMemberDao.isUserAdminOfTeam(teamId, userId)) {
            val adminCount = teamMemberDao.getAdminCountForTeam(teamId)
            if (adminCount <= 1) {
                return Result.failure(IllegalStateException("Cannot remove the last admin of the team"))
            }
        }

        if (team.serverId == null) {
            // If team has never been synced, just delete locally
            teamMemberDao.deleteTeamMember(teamId, userId)
        } else {
            // Mark for deletion during next sync
            teamMemberDao.markTeamMemberForDeletion(teamId, userId, System.currentTimeMillis())
        }

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTeamMembers()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after removing user", e)
            }
        }

        return Result.success(Unit)
    }

    override suspend fun changeUserRole(teamId: String, userId: String, newRole: String): Result<TeamMember> {
        if (newRole != "admin" && newRole != "member") {
            return Result.failure(IllegalArgumentException("Invalid role. Must be 'admin' or 'member'"))
        }

        val team = teamDao.getTeamByIdSync(teamId)
            ?: return Result.failure(NoSuchElementException("Team not found"))

        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        // Check if current user is admin of the team
        val isAdmin = teamMemberDao.isUserAdminOfTeam(teamId, currentUserId)
        if (!isAdmin) {
            return Result.failure(IllegalStateException("Only team admins can change roles"))
        }

        val member = teamMemberDao.getTeamMemberSync(teamId, userId)
            ?: return Result.failure(NoSuchElementException("User is not a member of this team"))

        // Cannot downgrade the last admin
        if (member.role == "admin" && newRole == "member") {
            val adminCount = teamMemberDao.getAdminCountForTeam(teamId)
            if (adminCount <= 1) {
                return Result.failure(IllegalStateException("Cannot downgrade the last admin of the team"))
            }
        }

        val updatedMember = member.copy(
            role = newRole,
            syncStatus = if (member.serverId == null) "pending_create" else "pending_update",
            lastModified = System.currentTimeMillis()
        )

        teamMemberDao.updateTeamMember(updatedMember)

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncTeamMembers()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after changing role", e)
            }
        }

        return Result.success(updatedMember.toDomainModel())
    }

    override fun getTeamMembers(teamId: String): Flow<List<TeamMember>> {
        return teamMemberDao.getTeamMembers(teamId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun isUserAdminOfTeam(teamId: String, userId: String): Flow<Boolean> {
        return teamMemberDao.isUserAdminOfTeamFlow(teamId, userId)
    }

    override fun isUserMemberOfTeam(teamId: String, userId: String): Flow<Boolean> {
        return teamMemberDao.isUserMemberOfTeamFlow(teamId, userId)
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
                        teamDao.updateTeamServerId(team.id, serverTeam.id.toString())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating team on server", e)
                }
            }

            // Process updates
            for (team in teamsToUpdate) {
                try {
                    if (team.serverId == null) continue

                    val response = apiService.updateTeam(
                        teamId = team.serverId.toLong(),
                        team = team.toApiRequest()
                    )

                    if (response.isSuccessful) {
                        teamDao.markTeamAsSynced(team.id)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating team on server", e)
                }
            }

            // Process deletes
            for (team in teamsToDelete) {
                try {
                    if (team.serverId == null) {
                        teamDao.deleteTeam(team.id)
                        continue
                    }

                    val response = apiService.deleteTeam(team.serverId.toLong())

                    if (response.isSuccessful) {
                        teamDao.deleteTeam(team.id)
                        teamMemberDao.deleteTeamMembers(team.id)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting team on server", e)
                }
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing teams", e)
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
                    val response = apiService.addTeamMember(
                        teamId = member.teamId.toLong(),
                        request = AddTeamMemberRequest(
                            user_id = member.userId,
                            role = member.role
                        )
                    )

                    if (response.isSuccessful && response.body() != null) {
                        val serverMember = response.body()!!
                        teamMemberDao.updateTeamMemberServerId(member.id, serverMember.id.toString())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating team member on server", e)
                }
            }

            // Process updates
            for (member in membersToUpdate) {
                try {
                    if (member.serverId == null) continue

                    val response = apiService.updateTeamMember(
                        teamId = member.teamId.toLong(),
                        memberId = member.serverId.toLong(),
                        role = member.role
                    )

                    if (response.isSuccessful) {
                        teamMemberDao.markTeamMemberAsSynced(member.id)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating team member on server", e)
                }
            }

            // Process deletes
            for (member in membersToDelete) {
                try {
                    if (member.serverId == null) {
                        teamMemberDao.deleteTeamMember(member.teamId, member.userId)
                        continue
                    }

                    val response = apiService.removeTeamMember(
                        teamId = member.teamId.toLong(),
                        memberId = member.serverId.toLong()
                    )

                    if (response.isSuccessful) {
                        teamMemberDao.deleteTeamMember(member.teamId, member.userId)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting team member on server", e)
                }
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing team members", e)
            return Result.failure(e)
        }
    }
}