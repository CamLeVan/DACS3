package com.example.taskapplication.ui.personal

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskapplication.domain.model.PersonalTask
import com.example.taskapplication.domain.repository.PersonalTaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException
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

    private val _showFilterDialog = MutableStateFlow(false)
    val showFilterDialog: StateFlow<Boolean> = _showFilterDialog

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _filterStatus = MutableStateFlow<String?>(null)
    val filterStatus: StateFlow<String?> = _filterStatus

    private val _filterPriority = MutableStateFlow<String?>(null)
    val filterPriority: StateFlow<String?> = _filterPriority

    private val _filterDueDateStart = MutableStateFlow<Long?>(null)
    val filterDueDateStart: StateFlow<Long?> = _filterDueDateStart

    private val _filterDueDateEnd = MutableStateFlow<Long?>(null)
    val filterDueDateEnd: StateFlow<Long?> = _filterDueDateEnd

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage

    private val _totalPages = MutableStateFlow(1)
    val totalPages: StateFlow<Int> = _totalPages

    private val _hasMorePages = MutableStateFlow(false)
    val hasMorePages: StateFlow<Boolean> = _hasMorePages

    private val perPage = 20

    init {
        loadTasks()
    }

    fun loadTasks() {
        viewModelScope.launch {
            _tasks.value = TasksState.Loading

            // Nếu có bộ lọc hoặc tìm kiếm, sử dụng API lọc và tìm kiếm
            if (_filterStatus.value != null || _filterPriority.value != null ||
                _filterDueDateStart.value != null || _filterDueDateEnd.value != null ||
                _searchQuery.value.isNotEmpty()) {

                loadFilteredTasks()
            } else {
                // Nếu không có bộ lọc, tải tất cả công việc từ local
                personalTaskRepository.getAllTasks()
                    .catch { e ->
                        _tasks.value = TasksState.Error(e.message ?: "Unknown error")
                    }
                    .collect { tasks ->
                        _tasks.value = TasksState.Success(tasks)
                    }
            }
        }
    }

    private suspend fun loadFilteredTasks() {
        try {
            _tasks.value = TasksState.Loading

            val result = personalTaskRepository.filterAndSearchTasksFromServer(
                status = _filterStatus.value,
                priority = _filterPriority.value,
                startDate = _filterDueDateStart.value,
                endDate = _filterDueDateEnd.value,
                query = if (_searchQuery.value.isNotEmpty()) _searchQuery.value else null,
                page = _currentPage.value,
                perPage = perPage
            )

            if (result.isSuccess) {
                val (tasks, meta) = result.getOrThrow()
                _tasks.value = TasksState.Success(tasks)

                // Cập nhật thông tin phân trang
                _totalPages.value = meta?.lastPage ?: 1
                _hasMorePages.value = meta != null && meta.currentPage < meta.lastPage
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"

                // Hiển thị thông báo lỗi thân thiện hơn
                val userFriendlyMessage = when {
                    errorMessage.contains("Lỗi kết nối đến máy chủ") -> errorMessage
                    errorMessage.contains("No network connection") -> "Không có kết nối mạng. Vui lòng kiểm tra kết nối và thử lại."
                    else -> "Đã xảy ra lỗi khi tải dữ liệu. Vui lòng thử lại sau."
                }

                _tasks.value = TasksState.Error(userFriendlyMessage)
                Log.e("PersonalTasksViewModel", "Error loading tasks: $errorMessage")
            }
        } catch (e: Exception) {
            val userFriendlyMessage = when (e) {
                is IOException -> "Lỗi kết nối mạng. Vui lòng kiểm tra kết nối và thử lại."
                else -> "Đã xảy ra lỗi không xác định. Vui lòng thử lại sau."
            }
            _tasks.value = TasksState.Error(userFriendlyMessage)
            Log.e("PersonalTasksViewModel", "Exception loading tasks", e)
        }
    }

    fun loadNextPage() {
        if (_hasMorePages.value) {
            viewModelScope.launch {
                try {
                    _currentPage.value += 1

                    val result = personalTaskRepository.filterAndSearchTasksFromServer(
                        status = _filterStatus.value,
                        priority = _filterPriority.value,
                        startDate = _filterDueDateStart.value,
                        endDate = _filterDueDateEnd.value,
                        query = if (_searchQuery.value.isNotEmpty()) _searchQuery.value else null,
                        page = _currentPage.value,
                        perPage = perPage
                    )

                    if (result.isSuccess) {
                        val (newTasks, meta) = result.getOrThrow()

                        // Thêm các công việc mới vào danh sách hiện tại
                        val currentTasks = (_tasks.value as? TasksState.Success)?.tasks ?: emptyList()
                        _tasks.value = TasksState.Success(currentTasks + newTasks)

                        // Cập nhật thông tin phân trang
                        _totalPages.value = meta?.lastPage ?: 1
                        _hasMorePages.value = meta != null && meta.currentPage < meta.lastPage
                    }
                } catch (e: Exception) {
                    // Xử lý lỗi nếu cần
                }
            }
        }
    }

    fun syncTasks() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                personalTaskRepository.syncTasks()
                // Sau khi đồng bộ, tải lại danh sách công việc
                resetFiltersAndLoadTasks()
            } catch (e: Exception) {
                // Xử lý lỗi nếu cần
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun searchTasks(query: String) {
        _searchQuery.value = query
        _currentPage.value = 1
        loadTasks()
    }

    fun filterTasks(status: String?, priority: String?, dueDateStart: Long?, dueDateEnd: Long?) {
        _filterStatus.value = status
        _filterPriority.value = priority
        _filterDueDateStart.value = dueDateStart
        _filterDueDateEnd.value = dueDateEnd
        _currentPage.value = 1
        loadTasks()
    }

    fun clearFilters() {
        _filterStatus.value = null
        _filterPriority.value = null
        _filterDueDateStart.value = null
        _filterDueDateEnd.value = null
        _searchQuery.value = ""
        _currentPage.value = 1
        loadTasks()
    }

    private fun resetFiltersAndLoadTasks() {
        clearFilters()
        loadTasks()
    }

    fun showFilterDialog() {
        _showFilterDialog.value = true
    }

    fun hideFilterDialog() {
        _showFilterDialog.value = false
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
            val currentTasks = (_tasks.value as? TasksState.Success)?.tasks ?: emptyList()
            val maxOrder = currentTasks.maxOfOrNull { it.order } ?: 0

            val taskWithOrder = task.copy(
                order = maxOrder + 1,
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

    sealed class TasksState {
        object Loading : TasksState()
        data class Success(val tasks: List<PersonalTask>) : TasksState()
        data class Error(val message: String) : TasksState()
    }
}
