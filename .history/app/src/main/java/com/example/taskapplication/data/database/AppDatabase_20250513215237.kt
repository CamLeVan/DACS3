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
import com.example.taskapplication.data.database.dao.TeamDocumentDao
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
        TeamRoleHistoryEntity::class,
        TeamDocumentEntity::class,
        DocumentFolderEntity::class,
        DocumentEntity::class,
        DocumentVersionEntity::class,
        DocumentPermissionEntity::class,
        UserInteractionEntity::class,
        AppSettingsEntity::class,
        AttachmentEntity::class
    ],
    version = 9,
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
    abstract fun documentFolderDao(): DocumentFolderDao
    abstract fun documentDao(): DocumentDao
    abstract fun documentVersionDao(): DocumentVersionDao
    abstract fun documentPermissionDao(): DocumentPermissionDao
    abstract fun userInteractionDao(): UserInteractionDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun attachmentDao(): AttachmentDao

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
                        `id` INTEGER PRIMARY KEY NOT NULL,
                        `teamId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT,
                        `fileUrl` TEXT NOT NULL,
                        `thumbnailUrl` TEXT,
                        `fileType` TEXT NOT NULL,
                        `fileSize` INTEGER NOT NULL,
                        `folderId` INTEGER,
                        `uploadedBy` INTEGER NOT NULL,
                        `uploadedAt` INTEGER NOT NULL,
                        `lastModified` INTEGER NOT NULL,
                        `serverId` INTEGER,
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

        // Migration from version 3 to version 4
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create document_folders table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `document_folders` (
                        `id` TEXT PRIMARY KEY NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `teamId` TEXT NOT NULL,
                        `parentFolderId` TEXT,
                        `createdBy` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `syncStatus` TEXT NOT NULL,
                        `isDeleted` INTEGER NOT NULL DEFAULT 0,
                        `serverId` TEXT,
                        FOREIGN KEY(`teamId`) REFERENCES `teams`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """
                )

                // Create documents table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `documents` (
                        `id` TEXT PRIMARY KEY NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `teamId` TEXT NOT NULL,
                        `folderId` TEXT,
                        `fileUrl` TEXT NOT NULL,
                        `fileType` TEXT NOT NULL,
                        `fileSize` INTEGER NOT NULL,
                        `uploadedBy` TEXT NOT NULL,
                        `uploadedAt` INTEGER NOT NULL,
                        `lastModified` INTEGER NOT NULL,
                        `accessLevel` TEXT NOT NULL,
                        `allowedUsers` TEXT NOT NULL,
                        `syncStatus` TEXT NOT NULL,
                        `isDeleted` INTEGER NOT NULL DEFAULT 0,
                        `serverId` TEXT,
                        `latestVersionId` TEXT,
                        FOREIGN KEY(`teamId`) REFERENCES `teams`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`folderId`) REFERENCES `document_folders`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """
                )

                // Create document_versions table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `document_versions` (
                        `id` TEXT PRIMARY KEY NOT NULL,
                        `documentId` TEXT NOT NULL,
                        `versionNumber` INTEGER NOT NULL,
                        `fileUrl` TEXT NOT NULL,
                        `fileSize` INTEGER NOT NULL,
                        `uploadedBy` TEXT NOT NULL,
                        `uploadedAt` INTEGER NOT NULL,
                        `changeNotes` TEXT NOT NULL,
                        `syncStatus` TEXT NOT NULL,
                        `serverId` TEXT,
                        FOREIGN KEY(`documentId`) REFERENCES `documents`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """
                )

                // Create document_permissions table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `document_permissions` (
                        `id` TEXT PRIMARY KEY NOT NULL,
                        `documentId` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `permissionType` TEXT NOT NULL,
                        `grantedBy` TEXT NOT NULL,
                        `grantedAt` INTEGER NOT NULL,
                        `syncStatus` TEXT NOT NULL,
                        `serverId` TEXT,
                        FOREIGN KEY(`documentId`) REFERENCES `documents`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """
                )

                // Create indices for better query performance
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_document_folders_teamId` ON `document_folders` (`teamId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_document_folders_parentFolderId` ON `document_folders` (`parentFolderId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_document_folders_createdBy` ON `document_folders` (`createdBy`)")

                database.execSQL("CREATE INDEX IF NOT EXISTS `index_documents_teamId` ON `documents` (`teamId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_documents_folderId` ON `documents` (`folderId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_documents_uploadedBy` ON `documents` (`uploadedBy`)")

                database.execSQL("CREATE INDEX IF NOT EXISTS `index_document_versions_documentId` ON `document_versions` (`documentId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_document_versions_uploadedBy` ON `document_versions` (`uploadedBy`)")

                database.execSQL("CREATE INDEX IF NOT EXISTS `index_document_permissions_documentId` ON `document_permissions` (`documentId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_document_permissions_userId` ON `document_permissions` (`userId`)")
            }
        }

        // Migration from version 4 to version 5
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Xóa bảng cũ nếu tồn tại
                database.execSQL("DROP TABLE IF EXISTS `team_documents`")

                // Tạo lại bảng với cấu trúc mới
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `team_documents` (
                        `id` INTEGER PRIMARY KEY NOT NULL,
                        `teamId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT,
                        `fileUrl` TEXT NOT NULL,
                        `thumbnailUrl` TEXT,
                        `fileType` TEXT NOT NULL,
                        `fileSize` INTEGER NOT NULL,
                        `folderId` INTEGER,
                        `uploadedBy` INTEGER NOT NULL,
                        `uploadedAt` INTEGER NOT NULL,
                        `lastModified` INTEGER NOT NULL,
                        `serverId` INTEGER,
                        `syncStatus` TEXT NOT NULL,
                        `accessLevel` TEXT NOT NULL,
                        `allowedUsers` TEXT NOT NULL,
                        `isDeleted` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`teamId`) REFERENCES `teams`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """
                )

                // Tạo lại các chỉ mục
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_team_documents_teamId` ON `team_documents` (`teamId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_team_documents_uploadedBy` ON `team_documents` (`uploadedBy`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_team_documents_folderId` ON `team_documents` (`folderId`)")
            }
        }

        // Migration from version 5 to version 6
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create user_interactions table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `user_interactions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `user_id` TEXT NOT NULL,
                        `interaction_type` TEXT NOT NULL,
                        `interaction_count` INTEGER NOT NULL DEFAULT 1,
                        `last_interaction_timestamp` INTEGER NOT NULL,
                        FOREIGN KEY(`user_id`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """
                )

                // Create app_settings table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `app_settings` (
                        `id` INTEGER PRIMARY KEY NOT NULL DEFAULT 1,
                        `current_user_id` TEXT,
                        `theme_mode` TEXT NOT NULL DEFAULT 'system',
                        `notification_enabled` INTEGER NOT NULL DEFAULT 1,
                        `last_sync_timestamp` INTEGER NOT NULL DEFAULT 0
                    )
                    """
                )

                // Create indices for better query performance
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_user_interactions_user_id` ON `user_interactions` (`user_id`)")
            }
        }

        // Migration from version 6 to version 7
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Cập nhật bảng messages để thêm trường clientTempId
                database.execSQL("ALTER TABLE `messages` ADD COLUMN `clientTempId` TEXT")

                // Tạo bảng attachments
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `attachments` (
                        `id` TEXT PRIMARY KEY NOT NULL,
                        `messageId` TEXT,
                        `fileName` TEXT NOT NULL,
                        `fileSize` INTEGER NOT NULL,
                        `fileType` TEXT NOT NULL,
                        `url` TEXT NOT NULL,
                        `serverId` TEXT,
                        `syncStatus` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`messageId`) REFERENCES `messages`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """
                )

                // Tạo chỉ mục cho bảng attachments
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_attachments_messageId` ON `attachments` (`messageId`)")
            }
        }

        // Migration from version 7 to version 8
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Xóa bảng cũ nếu tồn tại
                database.execSQL("DROP TABLE IF EXISTS `message_reactions`")

                // Tạo lại bảng với cấu trúc chính xác
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `message_reactions` (
                        `id` TEXT PRIMARY KEY NOT NULL,
                        `messageId` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `reaction` TEXT,
                        `serverId` TEXT,
                        `syncStatus` TEXT NOT NULL,
                        `lastModified` INTEGER NOT NULL
                    )
                    """
                )

                // Tạo chỉ mục cho bảng message_reactions
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_message_reactions_messageId` ON `message_reactions` (`messageId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_message_reactions_userId` ON `message_reactions` (`userId`)")
            }
        }

        // Migration from version 8 to version 9
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Xóa bảng cũ nếu tồn tại
                database.execSQL("DROP TABLE IF EXISTS `message_reactions`")

                // Tạo lại bảng với cấu trúc chính xác
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `message_reactions` (
                        `id` TEXT PRIMARY KEY NOT NULL,
                        `messageId` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `reaction` TEXT,
                        `serverId` TEXT,
                        `syncStatus` TEXT NOT NULL,
                        `lastModified` INTEGER NOT NULL
                    )
                    """
                )

                // Tạo chỉ mục cho bảng message_reactions
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_message_reactions_messageId` ON `message_reactions` (`messageId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_message_reactions_userId` ON `message_reactions` (`userId`)")
            }
        }
    }
}