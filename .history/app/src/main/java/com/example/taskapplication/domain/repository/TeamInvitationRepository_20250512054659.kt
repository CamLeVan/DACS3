package com.example.taskapplication.domain.repository

import com.example.taskapplication.domain.model.TeamInvitation
import kotlinx.coroutines.flow.Flow
import java.io.IOException

/**
 * Repository interface for managing team invitations
 */
interface TeamInvitationRepository {
    /**
     * Get all invitations for a team
     * @param teamId The ID of the team
     * @return Flow of list of team invitations
     */
    fun getTeamInvitations(teamId: String): Flow<List<TeamInvitation>>

    /**
     * Get all invitations for a team with specific status
     * @param teamId The ID of the team
     * @param status The status of invitations to get (pending, accepted, rejected)
     * @return Flow of list of team invitations
     */
    fun getTeamInvitationsByStatus(teamId: String, status: String): Flow<List<TeamInvitation>>

    /**
     * Get all invitations for the current user
     * @return Flow of list of team invitations
     */
    fun getUserInvitations(): Flow<List<TeamInvitation>>

    /**
     * Send an invitation to join a team
     * @param teamId The ID of the team
     * @param email The email of the user to invite
     * @param role The role to assign to the user
     * @return Result containing the created invitation or an error
     */
    suspend fun sendInvitation(teamId: String, email: String, role: String): Result<TeamInvitation>

    /**
     * Resend an invitation
     * @param invitationId The ID of the invitation to resend
     * @return Result containing the updated invitation or an error
     */
    suspend fun resendInvitation(invitationId: String): Result<TeamInvitation>

    /**
     * Update invitation status
     * @param invitationId The ID of the invitation to update
     * @param status The new status (accepted, rejected, cancelled)
     * @return Result containing the updated invitation or an error
     */
    suspend fun updateInvitationStatus(invitationId: String, status: String): Result<TeamInvitation>

    /**
     * Get invitation by ID
     * @param invitationId The ID of the invitation
     * @return Result containing the invitation or an error
     */
    suspend fun getInvitationById(invitationId: String): Result<TeamInvitation>

    /**
     * Accept an invitation to join a team
     * @param token The invitation token
     * @return Result containing success or an error
     */
    suspend fun acceptInvitation(token: String): Result<Unit>

    /**
     * Reject an invitation to join a team
     * @param token The invitation token
     * @return Result containing success or an error
     */
    suspend fun rejectInvitation(token: String): Result<Unit>

    /**
     * Cancel an invitation (by team manager)
     * @param teamId The ID of the team
     * @param invitationId The ID of the invitation
     * @return Result containing success or an error
     */
    suspend fun cancelInvitation(teamId: String, invitationId: String): Result<Unit>

    /**
     * Sync invitations with the server
     * @return Result containing success or an error
     */
    suspend fun syncInvitations(): Result<Unit>
}
