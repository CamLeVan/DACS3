package com.example.taskapplication.ui.personal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.domain.model.PersonalTask
import com.example.taskapplication.domain.repository.PersonalTaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class PersonalTasksViewModel @Inject constructor(
    private val personalTaskRepository: PersonalTaskRepository,
    private val apiService: ApiService
) : ViewModel() {

    // Trạng thái danh sách công việc
    private val _tasks = MutableStateFlow<TasksState>(TasksState.Loading)
    val tasks: StateFlow<TasksState> = _tasks

    // Trạng thái đang làm mới
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    // Trạng thái hiển thị dialog thêm công việc
    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog

    // Trạng thái hiển thị dialog lọc
    private val _showFilterDialog = MutableStateFlow(false)
    val showFilterDialog: StateFlow<Boolean> = _showFilterDialog

    // Trạng thái tìm kiếm
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Trạng thái lọc
    private val _filterStatus = MutableStateFlow<String?>(null)
    val filterStatus: StateFlow<String?> = _filterStatus

    private val _filterPriority = MutableStateFlow<String?>(null)
    val filterPriority: StateFlow<String?> = _filterPriority

    private val _filterDueDateStart = MutableStateFlow<Long?>(null)
    val filterDueDateStart: StateFlow<Long?> = _filterDueDateStart

    private val _filterDueDateEnd = MutableStateFlow<Long?>(null)
    val filterDueDateEnd: StateFlow<Long?> = _filterDueDateEnd

    // Trạng thái phân trang
    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage

    private val _hasMorePages = MutableStateFlow(false)
    val hasMorePages: StateFlow<Boolean> = _hasMorePages

    private val _totalPages = MutableStateFlow(1)
    val totalPages: StateFlow<Int> = _totalPages

    init {
        loadTasks()
    }

    /**
     * Tải danh sách công việc từ local database
     */
    fun loadTasks() {
        viewModelScope.launch {
            _tasks.value = TasksState.Loading
            personalTaskRepository.getAllTasks()
                .catch { e ->
                    _tasks.value = TasksState.Error(e.message ?: "Unknown error")
                }
                .collect { tasks ->
                    _tasks.value = TasksState.Success(tasks)
                }
        }
    }

    /**
     * Đồng bộ hóa dữ liệu với server
     */
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

    /**
     * Tìm kiếm công việc từ server
     */
    fun searchTasks(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            _tasks.value = TasksState.Loading
            _currentPage.value = 1

            try {
                val result = personalTaskRepository.searchTasksFromServer(
                    query = query,
                    page = 1,
                    perPage = 20
                )

                if (result.isSuccess) {
                    val (tasks, meta) = result.getOrNull()!!
                    _tasks.value = TasksState.Success(tasks)

                    // Cập nhật thông tin phân trang
                    _hasMorePages.value = meta != null && meta.currentPage < meta.lastPage
                    _totalPages.value = meta?.lastPage ?: 1
                } else {
                    _tasks.value = TasksState.Error("Lỗi khi tìm kiếm công việc")
                }
            } catch (e: Exception) {
                _tasks.value = TasksState.Error(e.message ?: "Lỗi không xác định")
            }
        }
    }

    /**
     * Lọc công việc từ server
     */
    fun filterTasks(
        status: String? = null,
        priority: String? = null,
        dueDateStart: Long? = null,
        dueDateEnd: Long? = null
    ) {
        _filterStatus.value = status
        _filterPriority.value = priority
        _filterDueDateStart.value = dueDateStart
        _filterDueDateEnd.value = dueDateEnd

        viewModelScope.launch {
            _tasks.value = TasksState.Loading
            _currentPage.value = 1

            // Chuyển đổi ngày thành chuỗi ISO
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val dueDateStartStr = dueDateStart?.let { dateFormat.format(Date(it)) }
            val dueDateEndStr = dueDateEnd?.let { dateFormat.format(Date(it)) }

            try {
                val response = apiService.getPersonalTasks(
                    status = status,
                    priority = priority,
                    dueDateStart = dueDateStartStr,
                    dueDateEnd = dueDateEndStr,
                    page = 1,
                    perPage = 20
                )

                if (response.isSuccessful && response.body() != null) {
                    val tasksResponse = response.body()!!
                    val tasks = tasksResponse.data
                    _tasks.value = TasksState.Success(tasks)

                    // Cập nhật thông tin phân trang
                    val meta = tasksResponse.meta
                    _hasMorePages.value = meta != null && meta.currentPage < meta.lastPage
                    _totalPages.value = meta?.lastPage ?: 1
                } else {
                    _tasks.value = TasksState.Error("Lỗi khi lọc công việc")
                }
            } catch (e: Exception) {
                _tasks.value = TasksState.Error(e.message ?: "Lỗi không xác định")
            }
        }
    }

    /**
     * Tải trang tiếp theo
     */
    fun loadNextPage() {
        if (_hasMorePages.value) {
            val nextPage = _currentPage.value + 1
            _currentPage.value = nextPage

            viewModelScope.launch {
                // Chuyển đổi ngày thành chuỗi ISO
                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }

                val dueDateStartStr = _filterDueDateStart.value?.let { dateFormat.format(Date(it)) }
                val dueDateEndStr = _filterDueDateEnd.value?.let { dateFormat.format(Date(it)) }

                try {
                    val response = apiService.getPersonalTasks(
                        status = _filterStatus.value,
                        priority = _filterPriority.value,
                        dueDateStart = dueDateStartStr,
                        dueDateEnd = dueDateEndStr,
                        search = if (_searchQuery.value.isNotEmpty()) _searchQuery.value else null,
                        page = nextPage,
                        perPage = 20
                    )

                    if (response.isSuccessful && response.body() != null) {
                        val tasksResponse = response.body()!!
                        val newTasks = tasksResponse.data.map { it.toEntity().toDomainModel() }

                        // Thêm các công việc mới vào danh sách hiện tại
                        val currentTasks = (_tasks.value as? TasksState.Success)?.tasks ?: emptyList()
                        _tasks.value = TasksState.Success(currentTasks + newTasks)

                        // Cập nhật thông tin phân trang
                        val meta = tasksResponse.meta
                        _hasMorePages.value = meta != null && meta.currentPage < meta.lastPage
                        _totalPages.value = meta?.lastPage ?: 1
                    }
                } catch (e: Exception) {
                    // Không cập nhật _tasks.value để giữ nguyên danh sách hiện tại
                }
            }
        }
    }

    /**
     * Xóa bộ lọc và tải lại tất cả công việc
     */
    fun clearFilters() {
        _filterStatus.value = null
        _filterPriority.value = null
        _filterDueDateStart.value = null
        _filterDueDateEnd.value = null
        _searchQuery.value = ""
        _currentPage.value = 1

        loadTasks()
    }

    /**
     * Chuyển đổi trạng thái hoàn thành của công việc
     */
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

    /**
     * Tạo công việc mới
     */
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

    /**
     * Cập nhật công việc
     */
    fun updateTask(task: PersonalTask) {
        viewModelScope.launch {
            personalTaskRepository.updateTask(task)
        }
    }

    /**
     * Xóa công việc
     */
    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            personalTaskRepository.deleteTask(taskId)
        }
    }

    /**
     * Hiển thị dialog thêm công việc
     */
    fun showAddTaskDialog() {
        _showAddDialog.value = true
    }

    /**
     * Ẩn dialog thêm công việc
     */
    fun hideAddTaskDialog() {
        _showAddDialog.value = false
    }

    /**
     * Hiển thị dialog lọc
     */
    fun showFilterDialog() {
        _showFilterDialog.value = true
    }

    /**
     * Ẩn dialog lọc
     */
    fun hideFilterDialog() {
        _showFilterDialog.value = false
    }

    /**
     * Trạng thái danh sách công việc
     */
    sealed class TasksState {
        object Loading : TasksState()
        data class Success(val tasks: List<PersonalTask>) : TasksState()
        data class Error(val message: String) : TasksState()
    }
}
