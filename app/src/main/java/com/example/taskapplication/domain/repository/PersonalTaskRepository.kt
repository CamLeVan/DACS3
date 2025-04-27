package com.example.taskapplication.domain.repository

import com.example.taskapplication.domain.model.PersonalTask
import kotlinx.coroutines.flow.Flow

interface PersonalTaskRepository {
    fun getAllTasks(): Flow<List<PersonalTask>>
    suspend fun getTask(id: String): PersonalTask?
    suspend fun createTask(task: PersonalTask): Result<PersonalTask>
    suspend fun updateTask(task: PersonalTask): Result<PersonalTask>
    suspend fun deleteTask(taskId: String): Result<Unit>
    suspend fun syncTasks(): Result<Unit>
} 