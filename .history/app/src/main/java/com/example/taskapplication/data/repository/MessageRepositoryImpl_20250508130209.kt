package com.example.taskapplication.data.repository

import android.content.Context
import android.util.Log
import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.database.dao.MessageDao
import com.example.taskapplication.data.database.dao.MessageReactionDao
import com.example.taskapplication.data.database.dao.MessageReadStatusDao
import com.example.taskapplication.data.database.dao.TeamDao
import com.example.taskapplication.data.database.dao.TeamMemberDao
import com.example.taskapplication.data.mapper.toApiRequest
import com.example.taskapplication.data.mapper.toDomainModel
import com.example.taskapplication.data.mapper.toEntity
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.domain.model.Message
import com.example.taskapplication.domain.model.MessageReaction
import com.example.taskapplication.domain.model.MessageReadStatus
import com.example.taskapplication.domain.repository.MessageRepository
import com.example.taskapplication.util.NetworkUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val teamDao: TeamDao,
    private val teamMemberDao: TeamMemberDao,
    private val messageReadStatusDao: MessageReadStatusDao,
    private val messageReactionDao: MessageReactionDao,
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager,
    private val connectionChecker: ConnectionChecker,
    private val context: Context,
    private val networkUtils: NetworkUtils
) : MessageRepository {

    private val TAG = "MessageRepository"

    override fun getTeamMessages(teamId: String): Flow<List<Message>> {
        return messageDao.getTeamMessages(teamId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getDirectMessages(userId: String): Flow<List<Message>> {
        return messageDao.getDirectMessages(userId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getMessageById(id: String): Message? {
        return messageDao.getMessage(id)?.toDomainModel()
    }

    override suspend fun sendMessage(message: Message): Result<Message> {
        return try {
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.sendMessage(message.toEntity().toApiRequest())
                if (response.isSuccessful) {
                    response.body()?.let { messageResponse ->
                        val entity = messageResponse.toEntity()
                        messageDao.insertMessage(entity)
                        Result.success(entity.toDomainModel())
                    } ?: Result.failure(Exception("Empty response body"))
                } else {
                    Result.failure(Exception("Failed to send message: ${response.code()}"))
                }
            } else {
                val entity = message.toEntity()
                messageDao.insertMessage(entity)
                Result.success(entity.toDomainModel())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateMessage(message: Message): Result<Message> {
        return try {
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.updateMessage(message.toEntity().toApiRequest())
                if (response.isSuccessful) {
                    response.body()?.let { messageResponse ->
                        val entity = messageResponse.toEntity()
                        messageDao.updateMessage(entity)
                        Result.success(entity.toDomainModel())
                    } ?: Result.failure(Exception("Empty response body"))
                } else {
                    Result.failure(Exception("Failed to update message: ${response.code()}"))
                }
            } else {
                val entity = message.toEntity()
                messageDao.updateMessage(entity)
                Result.success(entity.toDomainModel())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMessage(messageId: Long): Result<Unit> {
        return try {
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.deleteMessage(messageId)
                if (response.isSuccessful) {
                    messageDao.deleteMessage(messageId)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to delete message: ${response.code()}"))
                }
            } else {
                messageDao.deleteMessage(messageId)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markMessageAsRead(messageId: Long, userId: String): Result<Unit> {
        return try {
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.markMessageAsRead(messageId, userId)
                if (response.isSuccessful) {
                    messageDao.markMessageAsRead(messageId, userId)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to mark message as read: ${response.code()}"))
                }
            } else {
                messageDao.markMessageAsRead(messageId, userId)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncMessages(): Result<Unit> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                return Result.failure(Exception("No network connection"))
            }

            // Sync pending messages
            val pendingMessages = messageDao.getPendingMessages()
            for (message in pendingMessages) {
                when (message.syncStatus) {
                    "pending_create" -> {
                        val response = apiService.sendMessage(message.toApiRequest())
                        if (response.isSuccessful) {
                            response.body()?.let { messageResponse ->
                                val entity = messageResponse.toEntity()
                                messageDao.updateMessage(entity)
                            }
                        }
                    }
                    "pending_update" -> {
                        val response = apiService.updateMessage(message.toApiRequest())
                        if (response.isSuccessful) {
                            response.body()?.let { messageResponse ->
                                val entity = messageResponse.toEntity()
                                messageDao.updateMessage(entity)
                            }
                        }
                    }
                    "pending_delete" -> {
                        val response = apiService.deleteMessage(message.id)
                        if (response.isSuccessful) {
                            messageDao.deleteMessage(message.id)
                        }
                    }
                }
            }

            // Sync server messages
            val response = apiService.getMessages()
            if (response.isSuccessful) {
                response.body()?.let { messages ->
                    for (messageResponse in messages) {
                        val existingMessage = messageDao.getMessageByServerId(messageResponse.id)
                        val entity = messageResponse.toEntity(existingMessage)
                        if (existingMessage == null) {
                            messageDao.insertMessage(entity)
                        } else {
                            messageDao.updateMessage(entity)
                        }
                    }
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to sync messages: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncTeamMessages(teamId: String): Result<Unit> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                return Result.failure(Exception("No network connection"))
            }

            // Sync pending team messages
            val pendingMessages = messageDao.getPendingTeamMessages(teamId)
            for (message in pendingMessages) {
                when (message.syncStatus) {
                    "pending_create" -> {
                        val response = apiService.sendMessage(message.toApiRequest())
                        if (response.isSuccessful) {
                            response.body()?.let { messageResponse ->
                                val entity = messageResponse.toEntity()
                                messageDao.updateMessage(entity)
                            }
                        }
                    }
                    "pending_update" -> {
                        val response = apiService.updateMessage(message.toApiRequest())
                        if (response.isSuccessful) {
                            response.body()?.let { messageResponse ->
                                val entity = messageResponse.toEntity()
                                messageDao.updateMessage(entity)
                            }
                        }
                    }
                    "pending_delete" -> {
                        val response = apiService.deleteMessage(message.id)
                        if (response.isSuccessful) {
                            messageDao.deleteMessage(message.id)
                        }
                    }
                }
            }

            // Sync server team messages
            val response = apiService.getTeamMessages(teamId)
            if (response.isSuccessful) {
                response.body()?.let { messages ->
                    for (messageResponse in messages) {
                        val existingMessage = messageDao.getMessageByServerId(messageResponse.id)
                        val entity = messageResponse.toEntity(existingMessage)
                        if (existingMessage == null) {
                            messageDao.insertMessage(entity)
                        } else {
                            messageDao.updateMessage(entity)
                        }
                    }
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to sync team messages: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveMessage(message: Message) {
        try {
            messageDao.insertMessage(message.toEntity())
        } catch (e: Exception) {
            Log.e(TAG, "Error saving message", e)
        }
    }

    override suspend fun addReaction(messageId: String, reaction: String): Result<MessageReaction> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                val entity = MessageReaction(
                    id = java.util.UUID.randomUUID().toString(),
                    messageId = messageId,
                    reaction = reaction,
                    syncStatus = "pending"
                ).toEntity()
                messageReactionDao.insertReaction(entity)
                Result.success(entity.toDomainModel())
            } else {
                val response = apiService.addReaction(messageId, reaction)
                if (response.isSuccessful) {
                    response.body()?.let { reactionResponse ->
                        val entity = reactionResponse.toEntity()
                        messageReactionDao.insertReaction(entity)
                        Result.success(entity.toDomainModel())
                    } ?: Result.failure(Exception("Empty response body"))
                } else {
                    Result.failure(Exception("Failed to add reaction: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeReaction(messageId: String, reactionId: String): Result<Unit> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                messageReactionDao.deleteReaction(reactionId)
                Result.success(Unit)
            } else {
                val response = apiService.removeReaction(messageId, reactionId)
                if (response.isSuccessful) {
                    messageReactionDao.deleteReaction(reactionId)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to remove reaction: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveReadStatus(readStatus: MessageReadStatus) {
        try {
            messageReadStatusDao.insertReadStatus(readStatus.toEntity())
        } catch (e: Exception) {
            Log.e(TAG, "Error saving read status", e)
        }
    }

    override suspend fun getOlderTeamMessages(teamId: String, olderThan: Long, limit: Int): Result<List<Message>> {
        return try {
            val messages = messageDao.getOlderTeamMessages(teamId, olderThan, limit)
            Result.success(messages.map { it.toDomainModel() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markMessageAsDeleted(messageId: Long) {
        try {
            messageDao.markMessageAsDeleted(messageId)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking message as deleted", e)
        }
    }
}