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
    version = 2,
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
}