package com.example.taskapplication.domain.repository

import com.example.taskapplication.domain.model.Message
import com.example.taskapplication.domain.model.MessageReaction
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun getTeamMessages(teamId: String): Flow<List<Message>>
    fun getDirectMessages(userId1: String, userId2: String): Flow<List<Message>>
    suspend fun getMessageById(id: String): Message?
    suspend fun sendTeamMessage(teamId: String, content: String): Result<Message>
    suspend fun sendDirectMessage(receiverId: String, content: String): Result<Message>
    suspend fun updateMessage(message: Message): Result<Message>
    suspend fun deleteMessage(messageId: String): Result<Unit>
    suspend fun markMessageAsRead(messageId: String): Result<Unit>
    suspend fun addReaction(messageId: String, reaction: String): Result<MessageReaction>
    suspend fun removeReaction(messageId: String, reactionId: String): Result<Unit>
    suspend fun getUnreadMessageCount(): Result<Map<String, Int>>
    suspend fun getOlderTeamMessages(teamId: String, olderThan: Long, limit: Int): Result<List<Message>>
    suspend fun syncMessages(): Result<Unit>
    suspend fun syncTeamMessages(teamId: String): Result<Unit>
} 