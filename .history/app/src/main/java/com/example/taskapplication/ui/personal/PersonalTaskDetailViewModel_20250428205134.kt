package com.example.taskapplication.ui.personal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskapplication.domain.model.PersonalTask
import com.example.taskapplication.domain.repository.PersonalTaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel cho màn hình chi tiết công việc cá nhân
 */
@HiltViewModel
class PersonalTaskDetailViewModel @Inject constructor(
    private val personalTaskRepository: PersonalTaskRepository
) : ViewModel() {
    
    /**
     * Lấy thông tin chi tiết của một công việc
     * @param taskId ID của công việc cần lấy
     * @return Đối tượng PersonalTask hoặc null nếu không tìm thấy
     */
    suspend fun getTask(taskId: String): PersonalTask? {
        return personalTaskRepository.getTask(taskId)
    }
    
    /**
     * Cập nhật trạng thái hoàn thành của công việc
     * @param task Công việc cần cập nhật
     */
    fun toggleTaskCompletion(task: PersonalTask) {
        viewModelScope.launch {
            val updatedTask = task.copy(
                isCompleted = !task.isCompleted,
                syncStatus = "pending_update",
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
}
