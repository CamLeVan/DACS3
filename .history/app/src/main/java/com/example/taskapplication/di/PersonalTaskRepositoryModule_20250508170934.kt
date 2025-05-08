package com.example.taskapplication.di

import com.example.taskapplication.domain.repository.PersonalTaskRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PersonalTaskRepositoryModule {

    @Provides
    @Singleton
    fun providePersonalTaskRepository(): PersonalTaskRepository {
        // Temporary implementation until we fix the real repository
        return object : PersonalTaskRepository {
            override fun getAllTasks() = kotlinx.coroutines.flow.flow { emit(emptyList<com.example.taskapplication.domain.model.PersonalTask>()) }
            override fun getTaskById(id: String) = kotlinx.coroutines.flow.flow { emit(null) }
            override suspend fun addTask(task: com.example.taskapplication.domain.model.PersonalTask) = Result.success(task)
            override suspend fun updateTask(task: com.example.taskapplication.domain.model.PersonalTask) = Result.success(task)
            override suspend fun deleteTask(id: String) = Result.success(Unit)
            override suspend fun syncTasks() = Result.success(Unit)
        }
    }
}
