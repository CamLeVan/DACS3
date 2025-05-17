package com.example.taskapplication.domain.repository

import com.example.taskapplication.data.api.response.PaginationMeta
import com.example.taskapplication.domain.model.PersonalTask
import com.example.taskapplication.domain.model.Subtask
import kotlinx.coroutines.flow.Flow

interface PersonalTaskRepository {
    // Basic CRUD operations
    fun getAllTasks(): Flow<List<PersonalTask>>
    suspend fun getTask(id: String): PersonalTask?
    suspend fun createTask(task: PersonalTask): Result<PersonalTask>
    suspend fun updateTask(task: PersonalTask): Result<PersonalTask>
    suspend fun deleteTask(taskId: String): Result<Unit>
    suspend fun syncTasks(): Result<Unit>

    // Task filtering
    fun getTasksByPriority(priority: String): Flow<List<PersonalTask>>
    fun getTasksByStatus(status: String): Flow<List<PersonalTask>>
    fun getTasksByDueDateRange(startDate: Long, endDate: Long): Flow<List<PersonalTask>>
    fun getOverdueTasks(): Flow<List<PersonalTask>>
    fun getTasksDueToday(): Flow<List<PersonalTask>>
    fun getTasksDueThisWeek(): Flow<List<PersonalTask>>
    fun searchTasks(query: String): Flow<List<PersonalTask>>

    // Task ordering
    suspend fun updateTaskOrder(taskId: String, newOrder: Int): Result<Unit>
    suspend fun updateTasksOrder(taskOrders: Map<String, Int>): Result<Unit>

    // Server operations
    suspend fun filterTasksFromServer(
        status: String? = null,
        priority: String? = null,
        startDate: Long? = null,
        endDate: Long? = null,
        page: Int? = null,
        perPage: Int? = null
    ): Result<Pair<List<PersonalTask>, PaginationMeta?>>

    suspend fun searchTasksFromServer(
        query: String,
        page: Int? = null,
        perPage: Int? = null
    ): Result<Pair<List<PersonalTask>, PaginationMeta?>>

    /**
     * Lọc và tìm kiếm công việc từ server
     * @param status Trạng thái công việc
     * @param priority Độ ưu tiên công việc
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @param query Từ khóa tìm kiếm
     * @param page Trang hiện tại
     * @param perPage Số lượng công việc trên mỗi trang
     * @return Cặp danh sách công việc và thông tin phân trang
     */
    suspend fun filterAndSearchTasksFromServer(
        status: String? = null,
        priority: String? = null,
        startDate: Long? = null,
        endDate: Long? = null,
        query: String? = null,
        page: Int? = null,
        perPage: Int? = null
    ): Result<Pair<List<PersonalTask>, PaginationMeta?>>

    /**
     * Lọc và tìm kiếm công việc từ cơ sở dữ liệu cục bộ
     * @param status Trạng thái công việc
     * @param priority Độ ưu tiên công việc
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @param query Từ khóa tìm kiếm
     * @return Flow danh sách công việc
     */
    fun filterAndSearchTasksLocally(
        status: String? = null,
        priority: String? = null,
        startDate: Long? = null,
        endDate: Long? = null,
        query: String? = null
    ): Flow<List<PersonalTask>>

    // Subtask operations
    fun getSubtasks(taskId: String): Flow<List<Subtask>>
    suspend fun getSubtask(subtaskId: String): Subtask?
    suspend fun createSubtask(taskId: String, subtask: Subtask): Result<Subtask>
    suspend fun updateSubtask(subtask: Subtask): Result<Subtask>
    suspend fun deleteSubtask(subtaskId: String): Result<Unit>
    suspend fun updateSubtaskOrder(subtaskId: String, newOrder: Int): Result<Unit>
    suspend fun updateSubtasksOrder(subtaskOrders: Map<String, Int>): Result<Unit>
}