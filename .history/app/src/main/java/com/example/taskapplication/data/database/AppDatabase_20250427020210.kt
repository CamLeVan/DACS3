package com.example.taskapplication.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.taskapplication.data.database.dao.*
import com.example.taskapplication.data.database.entities.*

@Database(
    entities = [
        UserEntity::class,
        PersonalTaskEntity::class,
        TeamEntity::class,
        TeamMemberEntity::class,
        TeamTaskEntity::class,
        MessageEntity::class,
        MessageReadStatusEntity::class,
        MessageReactionEntity::class,
        SyncMetadataEntity::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun personalTaskDao(): PersonalTaskDao
    abstract fun teamDao(): TeamDao
    abstract fun teamMemberDao(): TeamMemberDao
    abstract fun teamTaskDao(): TeamTaskDao
    abstract fun messageDao(): MessageDao
    abstract fun messageReadStatusDao(): MessageReadStatusDao
    abstract fun messageReactionDao(): MessageReactionDao
    abstract fun syncMetadataDao(): SyncMetadataDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "task_app_database"
                )
                .fallbackToDestructiveMigration() // For simplicity in development
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}