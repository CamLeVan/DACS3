package com.example.taskapplication.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.TypeConverters
import com.example.taskapplication.data.database.dao.*
import com.example.taskapplication.data.database.entities.*
import com.example.taskapplication.data.database.util.Converters

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
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
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

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add message tables
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS messages (
                        id TEXT PRIMARY KEY NOT NULL,
                        teamId TEXT NOT NULL,
                        senderId TEXT NOT NULL,
                        content TEXT NOT NULL,
                        type TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        serverId INTEGER,
                        syncStatus TEXT NOT NULL,
                        lastModified INTEGER NOT NULL
                    )
                """)

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS message_read_status (
                        id TEXT PRIMARY KEY NOT NULL,
                        messageId TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        isRead INTEGER NOT NULL,
                        readAt INTEGER,
                        serverId INTEGER,
                        syncStatus TEXT NOT NULL,
                        lastModified INTEGER NOT NULL,
                        FOREIGN KEY (messageId) REFERENCES messages (id) ON DELETE CASCADE
                    )
                """)

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS message_reactions (
                        id TEXT PRIMARY KEY NOT NULL,
                        messageId TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        emoji TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        serverId INTEGER,
                        syncStatus TEXT NOT NULL,
                        lastModified INTEGER NOT NULL,
                        FOREIGN KEY (messageId) REFERENCES messages (id) ON DELETE CASCADE
                    )
                """)
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add sync metadata table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS sync_metadata (
                        id TEXT PRIMARY KEY NOT NULL,
                        entityType TEXT NOT NULL,
                        lastSyncTimestamp INTEGER NOT NULL,
                        lastModified INTEGER NOT NULL
                    )
                """)

                // Add indexes for better query performance
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_teamId ON messages(teamId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_senderId ON messages(senderId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_message_read_status_messageId ON message_read_status(messageId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_message_reactions_messageId ON message_reactions(messageId)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "task_app_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}