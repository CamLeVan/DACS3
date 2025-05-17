package com.example.taskapplication.domain.repository

import com.example.taskapplication.data.api.response.PaginationMeta
import com.example.taskapplication.domain.model.PersonalTask
import kotlinx.coroutines.flow.Flow

interface PersonalTaskRepository {
    fun getAllTasks(): Flow<List<PersonalTask>>
    suspend fun getTask(id: String): PersonalTask?
    suspend fun createTask(task: PersonalTask): Result<PersonalTask>
    suspend fun updateTask(task: PersonalTask): Result<PersonalTask>
    suspend fun deleteTask(taskId: String): Result<Unit>
    suspend fun syncTasks(): Result<Unit>

    // Các phương thức mới
    fun getTasksByPriority(priority: String): Flow<List<PersonalTask>>
    fun getTasksByStatus(status: String): Flow<List<PersonalTask>>
    fun getTasksByDueDateRange(startDate: Long, endDate: Long): Flow<List<PersonalTask>>
    fun getOverdueTasks(): Flow<List<PersonalTask>>
    fun getTasksDueToday(): Flow<List<PersonalTask>>
    fun getTasksDueThisWeek(): Flow<List<PersonalTask>>
    fun searchTasks(query: String): Flow<List<PersonalTask>>

    suspend fun getTaskCategories(): Result<List<String>>
    suspend fun getTaskStatistics(period: String? = null, startDate: Long? = null, endDate: Long? = null): Result<Map<String, Any>>
    suspend fun batchUpdateTasks(tasks: List<PersonalTask>): Result<List<PersonalTask>>

    suspend fun getTasksFromServer(
        status: String? = null,
        priority: String? = null,
        category: String? = null,
        dueDateStart: Long? = null,
        dueDateEnd: Long? = null,
        search: String? = null,
        page: Int? = null,
        perPage: Int? = null
    ): Result<Pair<List<PersonalTask>, PaginationMeta?>>
}