package com.example.taskapplication.data.database.dao

import androidx.room.*
import com.example.taskapplication.data.database.entities.MessageReactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageReactionDao {
    @Query("SELECT * FROM message_reactions WHERE messageId = :messageId")
    fun getReactionsByMessage(messageId: String): Flow<List<MessageReactionEntity>>

    @Query("SELECT * FROM message_reactions WHERE userId = :userId")
    suspend fun getReactionsByUser(userId: String): List<MessageReactionEntity>

    @Query("SELECT * FROM message_reactions WHERE messageId = :messageId AND userId = :userId")
    suspend fun getUserReactionForMessage(messageId: String, userId: String): MessageReactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReaction(reaction: MessageReactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReactions(reactions: List<MessageReactionEntity>)

    @Update
    suspend fun updateReaction(reaction: MessageReactionEntity)

    @Delete
    suspend fun deleteReaction(reaction: MessageReactionEntity)

    @Query("DELETE FROM message_reactions WHERE id = :reactionId")
    suspend fun deleteReaction(reactionId: String)

    @Query("DELETE FROM message_reactions WHERE messageId = :messageId AND userId = :userId")
    suspend fun deleteUserReactionForMessage(messageId: String, userId: String)

    @Query("DELETE FROM message_reactions WHERE messageId = :messageId")
    suspend fun deleteAllReactionsForMessage(messageId: String)

    @Query("SELECT * FROM message_reactions WHERE id = :reactionId")
    suspend fun getReactionSync(reactionId: String): MessageReactionEntity?

    @Query("SELECT * FROM message_reactions WHERE serverId = :serverId")
    suspend fun getReactionByServerId(serverId: String): MessageReactionEntity?

    @Query("SELECT * FROM message_reactions WHERE serverId = :serverId")
    fun getReactionByServerIdSync(serverId: String): MessageReactionEntity?

    @Query("UPDATE message_reactions SET serverId = :serverId WHERE id = :reactionId")
    suspend fun updateReactionServerId(reactionId: String, serverId: String)

    // For sync
    @Query("SELECT * FROM message_reactions WHERE messageId IN (:messageIds)")
    suspend fun getReactionsForMessages(messageIds: List<String>): List<MessageReactionEntity>

    @Query("UPDATE message_reactions SET serverId = :serverId, lastModified = :timestamp WHERE id = :id")
    suspend fun markAsSynced(id: String, serverId: String, timestamp: Long = System.currentTimeMillis())
}