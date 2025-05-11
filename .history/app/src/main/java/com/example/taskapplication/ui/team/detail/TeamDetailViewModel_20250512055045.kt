package com.example.taskapplication.ui.team.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.domain.model.Team
import com.example.taskapplication.domain.model.TeamInvitation
import com.example.taskapplication.domain.model.TeamMember
import com.example.taskapplication.domain.model.User
import com.example.taskapplication.domain.repository.TeamInvitationRepository
import com.example.taskapplication.domain.repository.TeamRepository
import com.example.taskapplication.domain.repository.UserRepository
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
    private val userRepository: UserRepository,
    private val teamInvitationRepository: TeamInvitationRepository,
    private val dataStoreManager: DataStoreManager,
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

    // State for role change operation
    private val _roleChangeState = MutableStateFlow<RoleChangeState>(RoleChangeState.Idle)
    val roleChangeState: StateFlow<RoleChangeState> = _roleChangeState

    // State for remove member operation
    private val _removeMemberState = MutableStateFlow<RemoveMemberState>(RemoveMemberState.Idle)
    val removeMemberState: StateFlow<RemoveMemberState> = _removeMemberState

    // Current user ID
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId

    // Is current user admin
    private val _isCurrentUserAdmin = MutableStateFlow(false)
    val isCurrentUserAdmin: StateFlow<Boolean> = _isCurrentUserAdmin

    // Search results for users
    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults

    // Search state
    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState

    // Suggested users (frequent collaborators)
    private val _suggestedUsers = MutableStateFlow<List<User>>(emptyList())
    val suggestedUsers: StateFlow<List<User>> = _suggestedUsers

    // Suggested users state
    private val _suggestedUsersState = MutableStateFlow<SuggestedUsersState>(SuggestedUsersState.Loading)
    val suggestedUsersState: StateFlow<SuggestedUsersState> = _suggestedUsersState

    // Pending invitations
    private val _pendingInvitations = MutableStateFlow<List<TeamInvitation>>(emptyList())
    val pendingInvitations: StateFlow<List<TeamInvitation>> = _pendingInvitations

    // Invitations state
    private val _invitationsState = MutableStateFlow<InvitationsState>(InvitationsState.Loading)
    val invitationsState: StateFlow<InvitationsState> = _invitationsState

    // Resend invitation state
    private val _resendInvitationState = MutableStateFlow<ResendInvitationState>(ResendInvitationState.Idle)
    val resendInvitationState: StateFlow<ResendInvitationState> = _resendInvitationState

    init {
        loadTeam()
        loadTeamMembers()
        loadCurrentUser()
        checkCurrentUserAdmin()
        loadSuggestedUsers()
        loadPendingInvitations()
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
     * Load current user ID
     */
    private fun loadCurrentUser() {
        viewModelScope.launch {
            val userId = dataStoreManager.getCurrentUserId()
            _currentUserId.value = userId
        }
    }

    /**
     * Check if current user is admin of the team
     */
    private fun checkCurrentUserAdmin() {
        viewModelScope.launch {
            val userId = _currentUserId.value ?: return@launch

            teamRepository.isUserAdminOfTeam(teamId, userId)
                .catch { e ->
                    // Handle error, default to false
                    _isCurrentUserAdmin.value = false
                }
                .collect { isAdmin ->
                    _isCurrentUserAdmin.value = isAdmin
                }
        }
    }

    /**
     * Change user role
     */
    fun changeUserRole(userId: String, newRole: String) {
        viewModelScope.launch {
            _roleChangeState.value = RoleChangeState.Loading

            try {
                teamRepository.changeUserRole(teamId, userId, newRole)
                    .onSuccess {
                        _roleChangeState.value = RoleChangeState.Success
                        loadTeamMembers()
                    }
                    .onFailure { e ->
                        _roleChangeState.value = RoleChangeState.Error(e.message ?: "Failed to change role")
                    }
            } catch (e: Exception) {
                _roleChangeState.value = RoleChangeState.Error(e.message ?: "Failed to change role")
            }
        }
    }

    /**
     * Remove user from team
     */
    fun removeUserFromTeam(userId: String) {
        viewModelScope.launch {
            _removeMemberState.value = RemoveMemberState.Loading

            try {
                teamRepository.removeUserFromTeam(teamId, userId)
                    .onSuccess {
                        _removeMemberState.value = RemoveMemberState.Success
                        loadTeamMembers()
                    }
                    .onFailure { e ->
                        _removeMemberState.value = RemoveMemberState.Error(e.message ?: "Failed to remove member")
                    }
            } catch (e: Exception) {
                _removeMemberState.value = RemoveMemberState.Error(e.message ?: "Failed to remove member")
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

        if (_roleChangeState.value is RoleChangeState.Error) {
            _roleChangeState.value = RoleChangeState.Idle
        }

        if (_removeMemberState.value is RemoveMemberState.Error) {
            _removeMemberState.value = RemoveMemberState.Idle
        }

        if (_searchState.value is SearchState.Error) {
            _searchState.value = SearchState.Idle
        }
    }

    /**
     * Tìm kiếm người dùng theo tên hoặc email
     */
    fun searchUsers(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _searchResults.value = emptyList()
                _searchState.value = SearchState.Idle
                return@launch
            }

            _searchState.value = SearchState.Loading

            try {
                val results = userRepository.searchUsers(query)
                _searchResults.value = results
                _searchState.value = if (results.isEmpty()) {
                    SearchState.Empty
                } else {
                    SearchState.Success
                }
            } catch (e: Exception) {
                _searchState.value = SearchState.Error(e.message ?: "Failed to search users")
                _searchResults.value = emptyList()
            }
        }
    }

    /**
     * Xóa kết quả tìm kiếm
     */
    fun clearSearchResults() {
        _searchResults.value = emptyList()
        _searchState.value = SearchState.Idle
    }

    /**
     * Tải danh sách người dùng được gợi ý (người dùng thường xuyên tương tác)
     */
    private fun loadSuggestedUsers() {
        viewModelScope.launch {
            _suggestedUsersState.value = SuggestedUsersState.Loading

            try {
                val users = userRepository.getRecentCollaborators(5)

                _suggestedUsers.value = users
                _suggestedUsersState.value = if (users.isEmpty()) {
                    SuggestedUsersState.Empty
                } else {
                    SuggestedUsersState.Success
                }
            } catch (e: Exception) {
                _suggestedUsersState.value = SuggestedUsersState.Error(e.message ?: "Failed to load suggested users")
                _suggestedUsers.value = emptyList()
            }
        }
    }

    /**
     * Làm mới danh sách người dùng được gợi ý
     */
    fun refreshSuggestedUsers() {
        loadSuggestedUsers()
    }

    /**
     * Tải danh sách lời mời đang chờ
     */
    private fun loadPendingInvitations() {
        viewModelScope.launch {
            _invitationsState.value = InvitationsState.Loading

            try {
                teamInvitationRepository.getTeamInvitationsByStatus(teamId, "pending")
                    .catch { e ->
                        _invitationsState.value = InvitationsState.Error(e.message ?: "Failed to load invitations")
                        _pendingInvitations.value = emptyList()
                    }
                    .collect { invitations ->
                        _pendingInvitations.value = invitations
                        _invitationsState.value = if (invitations.isEmpty()) {
                            InvitationsState.Empty
                        } else {
                            InvitationsState.Success
                        }
                    }
            } catch (e: Exception) {
                _invitationsState.value = InvitationsState.Error(e.message ?: "Failed to load invitations")
                _pendingInvitations.value = emptyList()
            }
        }
    }

    /**
     * Làm mới danh sách lời mời
     */
    fun refreshInvitations() {
        loadPendingInvitations()
    }

    /**
     * Gửi lại lời mời
     */
    fun resendInvitation(invitationId: String) {
        viewModelScope.launch {
            _resendInvitationState.value = ResendInvitationState.Loading

            try {
                teamInvitationRepository.resendInvitation(invitationId)
                    .onSuccess {
                        _resendInvitationState.value = ResendInvitationState.Success
                        loadPendingInvitations() // Refresh invitations list
                    }
                    .onFailure { e ->
                        _resendInvitationState.value = ResendInvitationState.Error(e.message ?: "Failed to resend invitation")
                    }
            } catch (e: Exception) {
                _resendInvitationState.value = ResendInvitationState.Error(e.message ?: "Failed to resend invitation")
            }
        }
    }

    /**
     * Hủy lời mời
     */
    fun cancelInvitation(invitationId: String) {
        viewModelScope.launch {
            try {
                teamInvitationRepository.updateInvitationStatus(invitationId, "cancelled")
                    .onSuccess {
                        loadPendingInvitations() // Refresh invitations list
                    }
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }

    /**
     * Reset resend invitation state
     */
    fun resetResendInvitationState() {
        _resendInvitationState.value = ResendInvitationState.Idle
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

/**
 * State for role change operation
 */
sealed class RoleChangeState {
    object Idle : RoleChangeState()
    object Loading : RoleChangeState()
    object Success : RoleChangeState()
    data class Error(val message: String) : RoleChangeState()
}

/**
 * State for remove member operation
 */
sealed class RemoveMemberState {
    object Idle : RemoveMemberState()
    object Loading : RemoveMemberState()
    object Success : RemoveMemberState()
    data class Error(val message: String) : RemoveMemberState()
}

/**
 * State for search operation
 */
sealed class SearchState {
    object Idle : SearchState()
    object Loading : SearchState()
    object Success : SearchState()
    object Empty : SearchState()
    data class Error(val message: String) : SearchState()
}

/**
 * State for suggested users
 */
sealed class SuggestedUsersState {
    object Loading : SuggestedUsersState()
    object Success : SuggestedUsersState()
    object Empty : SuggestedUsersState()
    data class Error(val message: String) : SuggestedUsersState()
}

/**
 * State for invitations
 */
sealed class InvitationsState {
    object Loading : InvitationsState()
    object Success : InvitationsState()
    object Empty : InvitationsState()
    data class Error(val message: String) : InvitationsState()
}

/**
 * State for resend invitation operation
 */
sealed class ResendInvitationState {
    object Idle : ResendInvitationState()
    object Loading : ResendInvitationState()
    object Success : ResendInvitationState()
    data class Error(val message: String) : ResendInvitationState()
}
