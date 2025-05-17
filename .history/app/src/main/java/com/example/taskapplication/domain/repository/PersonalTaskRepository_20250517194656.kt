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

    // Subtask operations
    fun getSubtasks(taskId: String): Flow<List<Subtask>>
    suspend fun getSubtask(subtaskId: String): Subtask?
    suspend fun createSubtask(taskId: String, subtask: Subtask): Result<Subtask>
    suspend fun updateSubtask(subtask: Subtask): Result<Subtask>
    suspend fun deleteSubtask(subtaskId: String): Result<Unit>
    suspend fun updateSubtaskOrder(subtaskId: String, newOrder: Int): Result<Unit>
    suspend fun updateSubtasksOrder(subtaskOrders: Map<String, Int>): Result<Unit>
}