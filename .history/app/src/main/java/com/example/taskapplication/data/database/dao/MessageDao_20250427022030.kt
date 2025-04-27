package com.example.taskapplication.data.database.dao

import androidx.room.*
import com.example.taskapplication.data.database.entities.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE teamId = :teamId ORDER BY timestamp DESC")
    fun getTeamMessages(teamId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE (senderId = :userId1 AND receiverId = :userId2) OR (senderId = :userId2 AND receiverId = :userId1) ORDER BY timestamp DESC")
    fun getDirectMessages(userId1: String, userId2: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE senderId = :userId OR receiverId = :userId ORDER BY timestamp DESC")
    fun getAllUserMessages(userId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Delete
    suspend fun deleteMessage(message: MessageEntity)

    @Query("UPDATE messages SET syncStatus = 'pending_delete', lastModified = :timestamp WHERE id = :messageId")
    suspend fun markMessageForDeletion(messageId: String, timestamp: Long)

    @Query("DELETE FROM messages WHERE id = :messageId AND syncStatus = 'pending_create'")
    suspend fun deleteLocalOnlyMessage(messageId: String)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteSyncedMessage(messageId: String)

    @Query("UPDATE messages SET isDeleted = 1, lastModified = :timestamp WHERE serverId = :messageId")
    suspend fun markMessageAsDeleted(messageId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM messages WHERE syncStatus != 'synced'")
    suspend fun getPendingSyncMessages(): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE teamId = :teamId AND syncStatus != 'synced'")
    suspend fun getPendingSyncMessagesByTeam(teamId: String): List<MessageEntity>

    // For pagination
    @Query("SELECT * FROM messages WHERE teamId = :teamId AND timestamp < :olderThan ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getOlderTeamMessages(teamId: String, olderThan: Long, limit: Int): List<MessageEntity>
}