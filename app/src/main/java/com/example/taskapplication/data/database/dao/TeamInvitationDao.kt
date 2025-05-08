package com.example.taskapplication.data.database.dao

import androidx.room.*
import com.example.taskapplication.data.database.entities.TeamInvitationEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for team invitations
 */
@Dao
interface TeamInvitationDao {
    /**
     * Get all invitations for a team
     */
    @Query("SELECT * FROM team_invitations WHERE teamId = :teamId AND status = 'pending' AND syncStatus != 'pending_delete' ORDER BY createdAt DESC")
    fun getTeamInvitations(teamId: String): Flow<List<TeamInvitationEntity>>
    
    /**
     * Get all invitations for a user by email
     */
    @Query("SELECT * FROM team_invitations WHERE email = :email AND status = 'pending' AND syncStatus != 'pending_delete' ORDER BY createdAt DESC")
    fun getUserInvitationsByEmail(email: String): Flow<List<TeamInvitationEntity>>
    
    /**
     * Get invitation by ID
     */
    @Query("SELECT * FROM team_invitations WHERE id = :id")
    suspend fun getInvitationById(id: String): TeamInvitationEntity?
    
    /**
     * Get invitation by token
     */
    @Query("SELECT * FROM team_invitations WHERE token = :token")
    suspend fun getInvitationByToken(token: String): TeamInvitationEntity?
    
    /**
     * Insert a new invitation
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvitation(invitation: TeamInvitationEntity)
    
    /**
     * Update an invitation
     */
    @Update
    suspend fun updateInvitation(invitation: TeamInvitationEntity)
    
    /**
     * Delete an invitation
     */
    @Delete
    suspend fun deleteInvitation(invitation: TeamInvitationEntity)
    
    /**
     * Get all pending sync invitations
     */
    @Query("SELECT * FROM team_invitations WHERE syncStatus IN ('pending_create', 'pending_update', 'pending_delete')")
    suspend fun getPendingSyncInvitations(): List<TeamInvitationEntity>
    
    /**
     * Get invitation by server ID
     */
    @Query("SELECT * FROM team_invitations WHERE serverId = :serverId")
    suspend fun getInvitationByServerId(serverId: String): TeamInvitationEntity?
}
