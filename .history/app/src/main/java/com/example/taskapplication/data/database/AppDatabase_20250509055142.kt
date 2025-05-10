package com.example.taskapplication.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.taskapplication.data.database.dao.*
import com.example.taskapplication.data.database.entities.*
import com.example.taskapplication.data.database.util.Converters
import com.example.taskapplication.data.local.dao.TeamRoleHistoryDao
import com.example.taskapplication.data.local.entity.TeamRoleHistoryEntity

@Database(
    entities = [
        PersonalTaskEntity::class,
        TeamTaskEntity::class,
        MessageEntity::class,
        MessageReadStatusEntity::class,
        MessageReactionEntity::class,
        TeamEntity::class,
        TeamMemberEntity::class,
        UserEntity::class,
        TeamInvitationEntity::class,
        NotificationEntity::class,
        NotificationSettingsEntity::class,
        CalendarEventEntity::class,
        KanbanBoardEntity::class,
        KanbanColumnEntity::class,
        KanbanTaskEntity::class,
        TeamRoleHistoryEntity::class
    ],
    version = 3,
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
    abstract fun teamInvitationDao(): TeamInvitationDao
    abstract fun notificationDao(): NotificationDao
    abstract fun notificationSettingsDao(): NotificationSettingsDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun kanbanBoardDao(): KanbanBoardDao
    abstract fun kanbanColumnDao(): KanbanColumnDao
    abstract fun kanbanTaskDao(): KanbanTaskDao
    abstract fun teamRoleHistoryDao(): TeamRoleHistoryDao
    abstract fun teamDocumentDao(): TeamDocumentDao

    companion object {
        // Migration from version 1 to version 2
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create team_role_history table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `team_role_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `teamId` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `oldRole` TEXT NOT NULL,
                        `newRole` TEXT NOT NULL,
                        `changedByUserId` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `syncStatus` TEXT NOT NULL,
                        FOREIGN KEY(`teamId`) REFERENCES `teams`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """
                )

                // Create indices for better query performance
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_team_role_history_teamId` ON `team_role_history` (`teamId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_team_role_history_userId` ON `team_role_history` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_team_role_history_changedByUserId` ON `team_role_history` (`changedByUserId`)")
            }
        }

        // Migration from version 2 to version 3
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create team_documents table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `team_documents` (
                        `id` TEXT PRIMARY KEY NOT NULL,
                        `teamId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `fileUrl` TEXT NOT NULL,
                        `fileType` TEXT NOT NULL,
                        `fileSize` INTEGER NOT NULL,
                        `uploadedBy` TEXT NOT NULL,
                        `uploadedAt` INTEGER NOT NULL,
                        `lastModified` INTEGER NOT NULL,
                        `serverId` TEXT,
                        `syncStatus` TEXT NOT NULL,
                        `accessLevel` TEXT NOT NULL,
                        `allowedUsers` TEXT NOT NULL,
                        `isDeleted` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`teamId`) REFERENCES `teams`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """
                )

                // Create indices for better query performance
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_team_documents_teamId` ON `team_documents` (`teamId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_team_documents_uploadedBy` ON `team_documents` (`uploadedBy`)")
            }
        }
    }
}