package com.example.taskapplication.di

import com.example.taskapplication.data.repository.*
import com.example.taskapplication.data.repository.AuthRepositoryImpl
import com.example.taskapplication.data.repository.PersonalTaskRepositoryImpl
import com.example.taskapplication.data.repository.SyncRepositoryImpl
import com.example.taskapplication.data.repository.UserRepositoryImpl
import com.example.taskapplication.domain.repository.AuthRepository
import com.example.taskapplication.domain.repository.PersonalTaskRepository
import com.example.taskapplication.domain.repository.MessageRepository
import com.example.taskapplication.domain.repository.SyncRepository
import com.example.taskapplication.domain.repository.TeamRepository
import com.example.taskapplication.domain.repository.TeamTaskRepository
import com.example.taskapplication.domain.repository.UserRepository
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
    abstract fun bindPersonalTaskRepository(
        personalTaskRepositoryImpl: PersonalTaskRepositoryImpl
    ): PersonalTaskRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(
        syncRepositoryImpl: SyncRepositoryImpl
    ): SyncRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        notificationRepositoryImpl: com.example.taskapplication.data.repository.NotificationRepositoryImpl
    ): com.example.taskapplication.domain.repository.NotificationRepository

    @Binds
    @Singleton
    abstract fun bindTeamTaskRepository(
        teamTaskRepositoryImpl: com.example.taskapplication.data.repository.TeamTaskRepositoryImpl
    ): TeamTaskRepository

    @Binds
    @Singleton
    abstract fun bindMessageRepository(
        messageRepositoryImpl: com.example.taskapplication.data.repository.MessageRepositoryImpl
    ): MessageRepository

    @Binds
    @Singleton
    abstract fun bindTeamRepository(
        teamRepositoryImpl: com.example.taskapplication.data.repository.TeamRepositoryImpl
    ): TeamRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
}