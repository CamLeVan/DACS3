package com.example.taskapplication.ui.team.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskapplication.domain.model.TeamRoleHistory
import com.example.taskapplication.domain.repository.TeamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Team Role History screen
 */
@HiltViewModel
class TeamRoleHistoryViewModel @Inject constructor(
    private val teamRepository: TeamRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    // Team ID from navigation arguments
    private val teamId: String = checkNotNull(savedStateHandle.get<String>("teamId"))
    
    // State for role history
    private val _historyState = MutableStateFlow<RoleHistoryState>(RoleHistoryState.Loading)
    val historyState: StateFlow<RoleHistoryState> = _historyState
    
    // Optional user ID for filtering history by user
    private val userId: String? = savedStateHandle.get<String>("userId")
    
    init {
        loadRoleHistory()
    }
    
    /**
     * Load role history for the team or for a specific user in the team
     */
    fun loadRoleHistory() {
        viewModelScope.launch {
            _historyState.value = RoleHistoryState.Loading
            
            try {
                val historyFlow = if (userId != null) {
                    teamRepository.getRoleHistoryForUser(teamId, userId)
                } else {
                    teamRepository.getRoleHistoryForTeam(teamId)
                }
                
                historyFlow
                    .catch { e ->
                        _historyState.value = RoleHistoryState.Error(e.message ?: "Unknown error")
                    }
                    .collect { history ->
                        _historyState.value = if (history.isEmpty()) {
                            RoleHistoryState.Empty
                        } else {
                            RoleHistoryState.Success(history)
                        }
                    }
            } catch (e: Exception) {
                _historyState.value = RoleHistoryState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

/**
 * State for role history
 */
sealed class RoleHistoryState {
    object Loading : RoleHistoryState()
    object Empty : RoleHistoryState()
    data class Success(val history: List<TeamRoleHistory>) : RoleHistoryState()
    data class Error(val message: String) : RoleHistoryState()
}
