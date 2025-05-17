package com.example.taskapplication.ui.personal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskapplication.domain.model.PersonalTask
import com.example.taskapplication.domain.model.Subtask
import com.example.taskapplication.domain.repository.PersonalTaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PersonalTasksViewModel @Inject constructor(
    private val personalTaskRepository: PersonalTaskRepository
) : ViewModel() {

    private val _tasks = MutableStateFlow<TasksState>(TasksState.Loading)
    val tasks: StateFlow<TasksState> = _tasks

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog

    init {
        loadTasks()
    }

    fun loadTasks() {
        viewModelScope.launch {
            _tasks.value = TasksState.Loading
            personalTaskRepository.getAllTasks()
                .catch { e ->
                    _tasks.value = TasksState.Error(e.message ?: "Unknown error")
                }
                .collectLatest { tasks ->
                    // Cập nhật trạng thái overdue cho các task
                    val updatedTasks = tasks.map { task ->
                        if (task.isOverdue() && task.status != "completed") {
                            task.copy(status = "overdue")
                        } else {
                            task
                        }
                    }
                    _tasks.value = TasksState.Success(updatedTasks)
                }
        }
    }

    fun syncTasks() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                personalTaskRepository.syncTasks()
                // Sau khi đồng bộ, tải lại danh sách công việc
                loadTasks()
            } catch (e: Exception) {
                // Xử lý lỗi nếu cần
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun toggleTaskCompletion(task: PersonalTask) {
        viewModelScope.launch {
            val updatedTask = task.copy(
                status = if (task.status == "completed") "pending" else "completed",
                syncStatus = "pending_update",
                lastModified = System.currentTimeMillis()
            )
            personalTaskRepository.updateTask(updatedTask)
        }
    }

    fun createTask(task: PersonalTask) {
        viewModelScope.launch {
            // Lấy order lớn nhất hiện tại và tăng lên 1
            val maxOrder = personalTaskRepository.getTask("")?.order ?: 0
            val taskWithOrder = task.copy(
                syncStatus = "pending_create",
                lastModified = System.currentTimeMillis()
            )
            personalTaskRepository.createTask(taskWithOrder)
            hideAddTaskDialog()
        }
    }

    fun updateTask(task: PersonalTask) {
        viewModelScope.launch {
            personalTaskRepository.updateTask(task)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            personalTaskRepository.deleteTask(taskId)
        }
    }



    fun showAddTaskDialog() {
        _showAddDialog.value = true
    }

    fun hideAddTaskDialog() {
        _showAddDialog.value = false
    }

    fun filterTasksByStatus(status: String) {
        viewModelScope.launch {
            _tasks.value = TasksState.Loading
            personalTaskRepository.getTasksByStatus(status)
                .catch { e ->
                    _tasks.value = TasksState.Error(e.message ?: "Unknown error")
                }
                .collectLatest { tasks ->
                    _tasks.value = TasksState.Success(tasks)
                }
        }
    }

    fun filterTasksByPriority(priority: String) {
        viewModelScope.launch {
            _tasks.value = TasksState.Loading
            personalTaskRepository.getTasksByPriority(priority)
                .catch { e ->
                    _tasks.value = TasksState.Error(e.message ?: "Unknown error")
                }
                .collectLatest { tasks ->
                    _tasks.value = TasksState.Success(tasks)
                }
        }
    }

    fun searchTasks(query: String) {
        viewModelScope.launch {
            _tasks.value = TasksState.Loading
            personalTaskRepository.searchTasks(query)
                .catch { e ->
                    _tasks.value = TasksState.Error(e.message ?: "Unknown error")
                }
                .collectLatest { tasks ->
                    _tasks.value = TasksState.Success(tasks)
                }
        }
    }

    sealed class TasksState {
        object Loading : TasksState()
        data class Success(val tasks: List<PersonalTask>) : TasksState()
        data class Error(val message: String) : TasksState()
    }
}
