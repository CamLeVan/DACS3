package com.example.taskapplication.di

import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.database.dao.PersonalTaskDao
import com.example.taskapplication.data.repository.PersonalTaskRepositoryImpl
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.data.util.DataStoreManager
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
    fun providePersonalTaskRepository(
        personalTaskDao: PersonalTaskDao,
        apiService: ApiService,
        dataStoreManager: DataStoreManager,
        connectionChecker: ConnectionChecker
    ): PersonalTaskRepository {
        return PersonalTaskRepositoryImpl(
            personalTaskDao,
            apiService,
            dataStoreManager,
            connectionChecker
        )
    }
}
