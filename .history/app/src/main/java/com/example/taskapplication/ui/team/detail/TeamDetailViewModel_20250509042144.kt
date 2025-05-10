package com.example.taskapplication.ui.team.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.domain.model.Team
import com.example.taskapplication.domain.model.TeamMember
import com.example.taskapplication.domain.repository.TeamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Team Detail screen
 * Manages the state and data for displaying team details and members
 */
@HiltViewModel
class TeamDetailViewModel @Inject constructor(
    private val teamRepository: TeamRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Team ID from navigation arguments
    private val teamId: String = checkNotNull(savedStateHandle.get<String>("teamId"))

    // State for team details
    private val _teamState = MutableStateFlow<TeamDetailState>(TeamDetailState.Loading)
    val teamState: StateFlow<TeamDetailState> = _teamState

    // State for team members
    private val _membersState = MutableStateFlow<TeamMembersState>(TeamMembersState.Loading)
    val membersState: StateFlow<TeamMembersState> = _membersState

    // State for invite member operation
    private val _inviteState = MutableStateFlow<InviteState>(InviteState.Idle)
    val inviteState: StateFlow<InviteState> = _inviteState

    // State for showing invite dialog
    private val _showInviteDialog = MutableStateFlow(false)
    val showInviteDialog: StateFlow<Boolean> = _showInviteDialog

    init {
        loadTeam()
        loadTeamMembers()
    }

    /**
     * Load team details
     */
    fun loadTeam() {
        viewModelScope.launch {
            _teamState.value = TeamDetailState.Loading

            teamRepository.getTeamById(teamId)
                .catch { e ->
                    _teamState.value = TeamDetailState.Error(e.message ?: "Unknown error")
                }
                .collect { team ->
                    if (team != null) {
                        _teamState.value = TeamDetailState.Success(team)
                    } else {
                        _teamState.value = TeamDetailState.Error("Team not found")
                    }
                }
        }
    }

    /**
     * Load team members
     */
    fun loadTeamMembers() {
        viewModelScope.launch {
            _membersState.value = TeamMembersState.Loading

            teamRepository.getTeamMembers(teamId)
                .catch { e ->
                    _membersState.value = TeamMembersState.Error(e.message ?: "Unknown error")
                }
                .collect { members ->
                    if (members.isEmpty()) {
                        _membersState.value = TeamMembersState.Empty
                    } else {
                        _membersState.value = TeamMembersState.Success(members)
                    }
                }
        }
    }

    /**
     * Show the invite member dialog
     */
    fun showInviteDialog() {
        _showInviteDialog.value = true
    }

    /**
     * Hide the invite member dialog
     */
    fun hideInviteDialog() {
        _showInviteDialog.value = false
        // Reset invite state
        _inviteState.value = InviteState.Idle
    }

    /**
     * Invite a user to the team
     */
    fun inviteUserToTeam(email: String) {
        viewModelScope.launch {
            _inviteState.value = InviteState.Loading

            try {
                teamRepository.inviteUserToTeam(teamId, email)
                    .onSuccess {
                        _inviteState.value = InviteState.Success
                        hideInviteDialog()
                        loadTeamMembers()
                    }
                    .onFailure { e ->
                        _inviteState.value = InviteState.Error(e.message ?: "Failed to invite user")
                    }
            } catch (e: Exception) {
                _inviteState.value = InviteState.Error(e.message ?: "Failed to invite user")
            }
        }
    }

    /**
     * Reset error state
     */
    fun resetError() {
        if (_teamState.value is TeamDetailState.Error) {
            _teamState.value = TeamDetailState.Loading
            loadTeam()
        }

        if (_membersState.value is TeamMembersState.Error) {
            _membersState.value = TeamMembersState.Loading
            loadTeamMembers()
        }

        if (_inviteState.value is InviteState.Error) {
            _inviteState.value = InviteState.Idle
        }
    }
}

/**
 * State for team details
 */
sealed class TeamDetailState {
    object Loading : TeamDetailState()
    data class Success(val team: Team) : TeamDetailState()
    data class Error(val message: String) : TeamDetailState()
}

/**
 * State for team members
 */
sealed class TeamMembersState {
    object Loading : TeamMembersState()
    object Empty : TeamMembersState()
    data class Success(val members: List<TeamMember>) : TeamMembersState()
    data class Error(val message: String) : TeamMembersState()
}

/**
 * State for invite member operation
 */
sealed class InviteState {
    object Idle : InviteState()
    object Loading : InviteState()
    object Success : InviteState()
    data class Error(val message: String) : InviteState()
}
