package com.example.taskapplication.di

import android.content.Context
import androidx.room.Room
import com.example.taskapplication.data.database.AppDatabase
import com.example.taskapplication.data.database.dao.PersonalTaskDao
import com.example.taskapplication.data.database.dao.TeamTaskDao
import com.example.taskapplication.data.database.dao.MessageDao
import com.example.taskapplication.data.database.dao.MessageReadStatusDao
import com.example.taskapplication.data.database.dao.MessageReactionDao
import com.example.taskapplication.data.database.dao.TeamDao
import com.example.taskapplication.data.database.dao.TeamMemberDao
import com.example.taskapplication.data.database.dao.UserDao
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
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "task_application.db"
        ).build()
    }

    @Provides
    fun providePersonalTaskDao(database: AppDatabase): PersonalTaskDao {
        return database.personalTaskDao()
    }

    @Provides
    fun provideTeamTaskDao(database: AppDatabase): TeamTaskDao {
        return database.teamTaskDao()
    }

    @Provides
    fun provideMessageDao(database: AppDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    fun provideMessageReadStatusDao(database: AppDatabase): MessageReadStatusDao {
        return database.messageReadStatusDao()
    }

    @Provides
    fun provideMessageReactionDao(database: AppDatabase): MessageReactionDao {
        return database.messageReactionDao()
    }

    @Provides
    fun provideTeamDao(database: AppDatabase): TeamDao {
        return database.teamDao()
    }

    @Provides
    fun provideTeamMemberDao(database: AppDatabase): TeamMemberDao {
        return database.teamMemberDao()
    }

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }
}