package com.example.taskapplication.data.database.dao

import androidx.room.*
import com.example.taskapplication.data.database.entities.MessageReadStatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageReadStatusDao {
    @Query("SELECT * FROM message_read_status WHERE messageId = :messageId")
    fun getReadStatusByMessage(messageId: String): Flow<List<MessageReadStatusEntity>>

    @Query("SELECT * FROM message_read_status WHERE userId = :userId")
    suspend fun getReadStatusByUser(userId: String): List<MessageReadStatusEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadStatus(readStatus: MessageReadStatusEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadStatuses(readStatuses: List<MessageReadStatusEntity>)

    @Update
    suspend fun updateReadStatus(readStatus: MessageReadStatusEntity)

    @Delete
    suspend fun deleteReadStatus(readStatus: MessageReadStatusEntity)

    @Query("DELETE FROM message_read_status WHERE messageId = :messageId")
    suspend fun deleteReadStatusByMessage(messageId: String)

    @Query("SELECT * FROM message_read_status WHERE id = :readStatusId")
    suspend fun getReadStatus(readStatusId: String): MessageReadStatusEntity?

    @Query("SELECT * FROM message_read_status WHERE id = :readStatusId")
    fun getReadStatusSync(readStatusId: String): MessageReadStatusEntity?

    @Query("SELECT * FROM message_read_status WHERE serverId = :serverId")
    suspend fun getReadStatusByServerId(serverId: String): MessageReadStatusEntity?

    @Query("SELECT * FROM message_read_status WHERE serverId = :serverId")
    fun getReadStatusByServerIdSync(serverId: String): MessageReadStatusEntity?

    @Query("UPDATE message_read_status SET serverId = :serverId WHERE id = :readStatusId")
    suspend fun updateReadStatusServerId(readStatusId: String, serverId: String)

    @Query("UPDATE message_read_status SET syncStatus = 'synced' WHERE id = :readStatusId")
    suspend fun markReadStatusAsSynced(readStatusId: String)

    // Get all message IDs that have been read by a specific user
    @Query("SELECT messageId FROM message_read_status WHERE userId = :userId")
    suspend fun getReadMessageIdsByUser(userId: String): List<String>

    // For sync
    @Query("SELECT * FROM message_read_status WHERE messageId IN (:messageIds) AND userId = :userId")
    suspend fun getReadStatusForMessages(messageIds: List<String>, userId: String): List<MessageReadStatusEntity>

    @Query("SELECT * FROM message_read_status WHERE syncStatus = 'pending'")
    suspend fun getPendingReadStatuses(): List<MessageReadStatusEntity>

    @Query("UPDATE message_read_status SET syncStatus = 'synced', serverId = :serverId, lastModified = :timestamp WHERE id = :id")
    suspend fun markAsSynced(id: String, serverId: String, timestamp: Long = System.currentTimeMillis())
}