package com.example.taskapplication.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.taskapplication.data.database.dao.*
import com.example.taskapplication.data.database.entity.*
import com.example.taskapplication.data.database.util.Converters

@Database(
    entities = [
        PersonalTaskEntity::class,
        TeamTaskEntity::class,
        MessageEntity::class,
        MessageReadStatusEntity::class,
        MessageReactionEntity::class,
        TeamEntity::class,
        TeamMemberEntity::class,
        UserEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun personalTaskDao(): PersonalTaskDao
    abstract fun teamTaskDao(): TeamTaskDao
    abstract fun messageDao(): MessageDao
    abstract fun messageReadStatusDao(): MessageReadStatusDao
    abstract fun messageReactionDao(): MessageReactionDao
    abstract fun teamDao(): TeamDao
    abstract fun teamMemberDao(): TeamMemberDao
    abstract fun userDao(): UserDao
}