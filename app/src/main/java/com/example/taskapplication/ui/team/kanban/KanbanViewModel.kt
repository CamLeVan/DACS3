package com.example.taskapplication.ui.team.kanban

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskapplication.domain.model.KanbanBoard
import com.example.taskapplication.domain.model.KanbanColumn
import com.example.taskapplication.domain.model.KanbanTask
import com.example.taskapplication.domain.model.KanbanUser
import com.example.taskapplication.domain.model.TeamMember
import com.example.taskapplication.domain.repository.KanbanRepository
import com.example.taskapplication.domain.repository.TeamRepository
import com.example.taskapplication.domain.repository.TeamTaskRepository
import com.example.taskapplication.data.util.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for the Kanban Board screen
 */
@HiltViewModel
class KanbanViewModel @Inject constructor(
    private val kanbanRepository: KanbanRepository,
    private val teamRepository: TeamRepository,
    private val teamTaskRepository: TeamTaskRepository,
    private val dataStoreManager: DataStoreManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    // Team ID from navigation arguments
    private val teamId: String = checkNotNull(savedStateHandle.get<String>("teamId"))
    
    // State for kanban board
    private val _kanbanState = MutableStateFlow<KanbanState>(KanbanState.Loading)
    val kanbanState: StateFlow<KanbanState> = _kanbanState
    
    // State for team members
    private val _teamMembers = MutableStateFlow<List<TeamMember>>(emptyList())
    val teamMembers: StateFlow<List<TeamMember>> = _teamMembers
    
    // Current filter
    private val _currentFilter = MutableStateFlow(TaskFilter())
    val currentFilter: StateFlow<TaskFilter> = _currentFilter
    
    // Filtered kanban board
    val filteredKanbanState: StateFlow<KanbanState> = combine(
        kanbanState,
        currentFilter
    ) { state, filter ->
        if (state is KanbanState.Success) {
            val filteredBoard = applyFilterToBoard(state.board, filter)
            if (filteredBoard.columns.all { it.tasks.isEmpty() }) {
                KanbanState.Empty
            } else {
                KanbanState.Success(filteredBoard)
            }
        } else {
            state
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = KanbanState.Loading
    )
    
    init {
        loadKanbanBoard()
        loadTeamMembers()
    }
    
    /**
     * Load kanban board for the team
     */
    fun loadKanbanBoard() {
        viewModelScope.launch {
            _kanbanState.value = KanbanState.Loading
            
            kanbanRepository.getKanbanBoard(teamId)
                .catch { e ->
                    _kanbanState.value = KanbanState.Error(e.message ?: "Unknown error")
                }
                .collect { board ->
                    if (board == null) {
                        // Create a default board if none exists
                        createDefaultBoard()
                    } else {
                        _kanbanState.value = if (board.columns.isEmpty()) {
                            KanbanState.Empty
                        } else {
                            KanbanState.Success(board)
                        }
                    }
                }
        }
    }
    
    /**
     * Load team members
     */
    private fun loadTeamMembers() {
        viewModelScope.launch {
            teamRepository.getTeamMembers(teamId)
                .catch { e ->
                    // Handle error if needed
                }
                .collect { members ->
                    _teamMembers.value = members
                }
        }
    }
    
    /**
     * Create a default kanban board with standard columns
     */
    private fun createDefaultBoard() {
        viewModelScope.launch {
            // TODO: Implement creating a default board with standard columns
            // For now, just show empty state
            _kanbanState.value = KanbanState.Empty
        }
    }
    
    /**
     * Move a task to a different column or position
     */
    fun moveTask(taskId: String, columnId: String, position: Int) {
        viewModelScope.launch {
            kanbanRepository.moveTask(teamId, taskId, columnId, position)
                .onSuccess {
                    // Task moved successfully, board will be updated via Flow
                }
                .onFailure { e ->
                    // Handle error if needed
                }
        }
    }
    
    /**
     * Create a new task
     */
    fun createTask(
        title: String,
        description: String,
        dueDate: Long?,
        priority: String,
        assignedUserId: String?,
        columnId: String
    ) {
        viewModelScope.launch {
            // TODO: Implement creating a task
            // For now, just reload the board
            loadKanbanBoard()
        }
    }
    
    /**
     * Apply filter to tasks
     */
    fun applyFilter(assignedUserId: String?, priority: String?, isCompleted: Boolean?) {
        _currentFilter.value = TaskFilter(assignedUserId, priority, isCompleted)
    }
    
    /**
     * Apply filter to a kanban board
     */
    private fun applyFilterToBoard(board: KanbanBoard, filter: TaskFilter): KanbanBoard {
        val filteredColumns = board.columns.map { column ->
            val filteredTasks = column.tasks.filter { task ->
                var matches = true
                
                // Filter by assignee
                if (filter.assignedUserId != null) {
                    matches = matches && task.assignedTo?.id == filter.assignedUserId
                }
                
                // Filter by priority
                if (filter.priority != null) {
                    matches = matches && task.priority == filter.priority
                }
                
                // Filter by completion status
                if (filter.isCompleted != null) {
                    // TODO: Implement completion status filtering when available
                }
                
                matches
            }
            
            column.copy(tasks = filteredTasks)
        }
        
        return board.copy(columns = filteredColumns)
    }
}

/**
 * State for kanban board
 */
sealed class KanbanState {
    object Loading : KanbanState()
    object Empty : KanbanState()
    data class Success(val board: KanbanBoard) : KanbanState()
    data class Error(val message: String) : KanbanState()
}

/**
 * Filter for tasks
 */
data class TaskFilter(
    val assignedUserId: String? = null,
    val priority: String? = null,
    val isCompleted: Boolean? = null
)
