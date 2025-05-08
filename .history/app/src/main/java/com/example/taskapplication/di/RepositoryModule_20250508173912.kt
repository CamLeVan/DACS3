package com.example.taskapplication.di

import com.example.taskapplication.data.repository.AuthRepositoryImpl
import com.example.taskapplication.data.repository.PersonalTaskRepositoryImpl
import com.example.taskapplication.domain.repository.AuthRepository
import com.example.taskapplication.domain.repository.PersonalTaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindPersonalTaskRepository(
        personalTaskRepositoryImpl: PersonalTaskRepositoryImpl
    ): PersonalTaskRepository
}