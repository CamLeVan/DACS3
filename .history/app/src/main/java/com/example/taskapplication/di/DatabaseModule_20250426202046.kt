package com.example.taskapplication.di

import android.content.Context
import com.example.taskapplication.data.database.AppDatabase
import com.example.taskapplication.data.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideUserDao(appDatabase: AppDatabase): UserDao {
        return appDatabase.userDao()
    }

    @Provides
    fun providePersonalTaskDao(appDatabase: AppDatabase): PersonalTaskDao {
        return appDatabase.personalTaskDao()
    }

    @Provides
    fun provideTeamDao(appDatabase: AppDatabase): TeamDao {
        return appDatabase.teamDao()
    }

    @Provides
    fun provideTeamTaskDao(appDatabase: AppDatabase): TeamTaskDao {
        return appDatabase.teamTaskDao()
    }

    @Provides
    fun provideMessageDao(appDatabase: AppDatabase): MessageDao {
        return appDatabase.messageDao()
    }

    @Provides
    fun provideMessageReadStatusDao(appDatabase: AppDatabase): MessageReadStatusDao {
        return appDatabase.messageReadStatusDao()
    }

    @Provides
    fun provideMessageReactionDao(appDatabase: AppDatabase): MessageReactionDao {
        return appDatabase.messageReactionDao()
    }

    @Provides
    fun provideSyncMetadataDao(appDatabase: AppDatabase): SyncMetadataDao {
        return appDatabase.syncMetadataDao()
    }
} 