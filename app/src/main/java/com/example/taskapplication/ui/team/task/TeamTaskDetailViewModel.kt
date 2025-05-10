package com.example.taskapplication.ui.team.task

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskapplication.domain.model.TeamTask
import com.example.taskapplication.domain.repository.TeamTaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Team Task Detail screen
 * Manages the state and data for displaying team task details
 */
@HiltViewModel
class TeamTaskDetailViewModel @Inject constructor(
    private val teamTaskRepository: TeamTaskRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    // Task ID from navigation arguments
    private val taskId: String = checkNotNull(savedStateHandle.get<String>("taskId"))
    
    // State for task details
    private val _taskState = MutableStateFlow<TaskDetailState>(TaskDetailState.Loading)
    val taskState: StateFlow<TaskDetailState> = _taskState
    
    init {
        loadTask()
    }
    
    /**
     * Load task details
     */
    fun loadTask() {
        viewModelScope.launch {
            _taskState.value = TaskDetailState.Loading
            
            try {
                val task = teamTaskRepository.getTaskById(taskId)
                if (task != null) {
                    _taskState.value = TaskDetailState.Success(task)
                } else {
                    _taskState.value = TaskDetailState.Error("Task not found")
                }
            } catch (e: Exception) {
                _taskState.value = TaskDetailState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Toggle task completion status
     */
    fun toggleTaskCompletion() {
        viewModelScope.launch {
            val currentState = _taskState.value
            if (currentState is TaskDetailState.Success) {
                val task = currentState.task
                val updatedTask = task.copy(isCompleted = !task.isCompleted)
                
                teamTaskRepository.updateTask(updatedTask)
                    .onSuccess {
                        _taskState.value = TaskDetailState.Success(updatedTask)
                    }
                    .onFailure { e ->
                        // Show error but keep current state
                        _taskState.value = TaskDetailState.Error(e.message ?: "Failed to update task")
                        loadTask() // Reload task to ensure consistency
                    }
            }
        }
    }
    
    /**
     * Delete task
     */
    fun deleteTask(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                teamTaskRepository.deleteTask(taskId)
                    .onSuccess {
                        onSuccess()
                    }
                    .onFailure { e ->
                        _taskState.value = TaskDetailState.Error(e.message ?: "Failed to delete task")
                    }
            } catch (e: Exception) {
                _taskState.value = TaskDetailState.Error(e.message ?: "Failed to delete task")
            }
        }
    }
}

/**
 * State for task details
 */
sealed class TaskDetailState {
    object Loading : TaskDetailState()
    data class Success(val task: TeamTask) : TaskDetailState()
    data class Error(val message: String) : TaskDetailState()
}
