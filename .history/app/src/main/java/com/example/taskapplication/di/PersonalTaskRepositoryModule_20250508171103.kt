package com.example.taskapplication.di

import com.example.taskapplication.domain.model.PersonalTask
import com.example.taskapplication.domain.repository.PersonalTaskRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PersonalTaskRepositoryModule {

    @Provides
    @Singleton
    fun providePersonalTaskRepository(): PersonalTaskRepository {
        // Temporary implementation until we fix the real repository
        return object : PersonalTaskRepository {
            override fun getAllTasks(): Flow<List<PersonalTask>> = flow { emit(emptyList<PersonalTask>()) }
            override suspend fun getTask(id: String): PersonalTask? = null
            override suspend fun createTask(task: PersonalTask): Result<PersonalTask> = Result.success(task)
            override suspend fun updateTask(task: PersonalTask): Result<PersonalTask> = Result.success(task)
            override suspend fun deleteTask(taskId: String): Result<Unit> = Result.success(Unit)
            override suspend fun syncTasks(): Result<Unit> = Result.success(Unit)
        }
    }
}
