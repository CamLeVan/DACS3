package com.example.taskapplication.di

import android.content.Context
import androidx.room.Room
import com.example.taskapplication.data.database.AppDatabase
import com.example.taskapplication.data.database.dao.*
import com.example.taskapplication.data.local.dao.TeamRoleHistoryDao
import com.example.taskapplication.data.database.dao.KanbanBoardDao
import com.example.taskapplication.data.database.dao.KanbanColumnDao
import com.example.taskapplication.data.database.dao.KanbanTaskDao
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
            "task_app_database"
        )
        .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
        .build()
    }

    @Provides
    @Singleton
    fun providePersonalTaskDao(database: AppDatabase): PersonalTaskDao {
        return database.personalTaskDao()
    }

    @Provides
    @Singleton
    fun provideTeamTaskDao(database: AppDatabase): TeamTaskDao {
        return database.teamTaskDao()
    }

    @Provides
    @Singleton
    fun provideMessageDao(database: AppDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    @Singleton
    fun provideMessageReadStatusDao(database: AppDatabase): MessageReadStatusDao {
        return database.messageReadStatusDao()
    }

    @Provides
    @Singleton
    fun provideMessageReactionDao(database: AppDatabase): MessageReactionDao {
        return database.messageReactionDao()
    }

    @Provides
    @Singleton
    fun provideTeamDao(database: AppDatabase): TeamDao {
        return database.teamDao()
    }

    @Provides
    @Singleton
    fun provideTeamMemberDao(database: AppDatabase): TeamMemberDao {
        return database.teamMemberDao()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideTeamRoleHistoryDao(database: AppDatabase): TeamRoleHistoryDao {
        return database.teamRoleHistoryDao()
    }

    @Provides
    @Singleton
    fun provideKanbanBoardDao(database: AppDatabase): KanbanBoardDao {
        return database.kanbanBoardDao()
    }

    @Provides
    @Singleton
    fun provideKanbanColumnDao(database: AppDatabase): KanbanColumnDao {
        return database.kanbanColumnDao()
    }

    @Provides
    @Singleton
    fun provideKanbanTaskDao(database: AppDatabase): KanbanTaskDao {
        return database.kanbanTaskDao()
    }
}