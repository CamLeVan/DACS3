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

/**
 * ViewModel cho màn hình chi tiết công việc cá nhân
 */
@HiltViewModel
class PersonalTaskDetailViewModel @Inject constructor(
    private val personalTaskRepository: PersonalTaskRepository
) : ViewModel() {

    private val _subtasks = MutableStateFlow<List<Subtask>>(emptyList())
    val subtasks: StateFlow<List<Subtask>> = _subtasks

    private val _isLoadingSubtasks = MutableStateFlow(false)
    val isLoadingSubtasks: StateFlow<Boolean> = _isLoadingSubtasks

    /**
     * Lấy thông tin chi tiết của một công việc
     * @param taskId ID của công việc cần lấy
     * @return Đối tượng PersonalTask hoặc null nếu không tìm thấy
     */
    suspend fun getTask(taskId: String): PersonalTask? {
        return personalTaskRepository.getTask(taskId)
    }

    /**
     * Tải thông tin chi tiết của một công việc (không suspend)
     * @param taskId ID của công việc cần lấy
     */
    fun loadTask(taskId: String) {
        viewModelScope.launch {
            getTask(taskId)
        }
    }

    /**
     * Tải danh sách công việc con của một công việc
     * @param taskId ID của công việc chính
     */
    fun loadSubtasks(taskId: String) {
        viewModelScope.launch {
            _isLoadingSubtasks.value = true
            personalTaskRepository.getSubtasks(taskId)
                .catch { e ->
                    // Xử lý lỗi nếu cần
                    _isLoadingSubtasks.value = false
                }
                .collectLatest { subtaskList ->
                    _subtasks.value = subtaskList.sortedBy { it.order }
                    _isLoadingSubtasks.value = false
                }
        }
    }

    /**
     * Cập nhật trạng thái hoàn thành của công việc
     * @param task Công việc cần cập nhật
     */
    fun toggleTaskCompletion(task: PersonalTask) {
        viewModelScope.launch {
            val updatedTask = task.copy(
                status = if (task.status == "completed") "pending" else "completed",
                syncStatus = PersonalTask.SyncStatus.UPDATED,
                lastModified = System.currentTimeMillis()
            )
            personalTaskRepository.updateTask(updatedTask)
        }
    }

    /**
     * Cập nhật thông tin công việc
     * @param task Công việc với thông tin đã cập nhật
     */
    fun updateTask(task: PersonalTask) {
        viewModelScope.launch {
            personalTaskRepository.updateTask(task)
        }
    }

    /**
     * Xóa công việc
     * @param taskId ID của công việc cần xóa
     */
    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            personalTaskRepository.deleteTask(taskId)
        }
    }

    /**
     * Tạo công việc con mới
     * @param taskId ID của công việc chính
     * @param title Tiêu đề của công việc con
     */
    fun createSubtask(taskId: String, title: String) {
        viewModelScope.launch {
            // Lấy order lớn nhất hiện tại và tăng lên 1
            val maxOrder = _subtasks.value.maxOfOrNull { it.order } ?: 0

            val subtask = Subtask(
                id = UUID.randomUUID().toString(),
                taskableType = "App\\Models\\PersonalTask",
                taskableId = taskId,
                title = title,
                completed = false,
                order = maxOrder + 1,
                syncStatus = PersonalTask.SyncStatus.CREATED,
                lastModified = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )

            personalTaskRepository.createSubtask(taskId, subtask)

            // Cập nhật danh sách công việc con
            _subtasks.value = _subtasks.value + subtask
        }
    }

    /**
     * Cập nhật trạng thái hoàn thành của công việc con
     * @param subtask Công việc con cần cập nhật
     */
    fun toggleSubtaskCompletion(subtask: Subtask) {
        viewModelScope.launch {
            val updatedSubtask = subtask.copy(
                completed = !subtask.completed,
                syncStatus = PersonalTask.SyncStatus.UPDATED,
                lastModified = System.currentTimeMillis()
            )

            personalTaskRepository.updateSubtask(updatedSubtask)

            // Cập nhật danh sách công việc con
            _subtasks.value = _subtasks.value.map {
                if (it.id == updatedSubtask.id) updatedSubtask else it
            }
        }
    }

    /**
     * Xóa công việc con
     * @param subtaskId ID của công việc con cần xóa
     */
    fun deleteSubtask(subtaskId: String) {
        viewModelScope.launch {
            personalTaskRepository.deleteSubtask(subtaskId)

            // Cập nhật danh sách công việc con
            _subtasks.value = _subtasks.value.filter { it.id != subtaskId }
        }
    }

    /**
     * Cập nhật thứ tự của các công việc con
     * @param subtaskOrders Map chứa ID của công việc con và thứ tự mới
     */
    fun updateSubtasksOrder(subtaskOrders: Map<String, Int>) {
        viewModelScope.launch {
            personalTaskRepository.updateSubtasksOrder(subtaskOrders)

            // Cập nhật danh sách công việc con
            val updatedSubtasks = _subtasks.value.map { subtask ->
                subtaskOrders[subtask.id]?.let { newOrder ->
                    subtask.copy(
                        order = newOrder,
                        syncStatus = PersonalTask.SyncStatus.UPDATED,
                        lastModified = System.currentTimeMillis()
                    )
                } ?: subtask
            }

            _subtasks.value = updatedSubtasks.sortedBy { it.order }
        }
    }
}
