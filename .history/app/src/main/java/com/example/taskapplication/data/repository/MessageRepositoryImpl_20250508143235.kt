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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
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

    override fun getMessages(teamId: String): Flow<List<Message>> {
        return messageDao.getMessagesByTeam(teamId)
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                emit(emptyList())
            }
    }

    override fun getDirectMessages(userId1: String, userId2: String): Flow<List<Message>> {
        return messageDao.getDirectMessages(userId1, userId2)
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                emit(emptyList())
            }
    }

    override suspend fun getMessageById(id: String): Message? {
        return messageDao.getMessageById(id)?.toDomainModel()
    }

    override suspend fun createMessage(message: Message): Result<Message> {
        return try {
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.createMessage(message.toEntity().toApiRequest())
                if (response.isSuccessful) {
                    response.body()?.let { messageResponse ->
                        val entity = messageResponse.toEntity()
                        messageDao.insertMessage(entity)
                        Result.success(entity.toDomainModel())
                    } ?: Result.failure(IOException("Empty response from server"))
                } else {
                    val entity = message.toEntity().copy(syncStatus = "pending_create")
                    messageDao.insertMessage(entity)
                    Result.success(entity.toDomainModel())
                }
            } else {
                val entity = message.toEntity().copy(syncStatus = "pending_create")
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
                val response = apiService.updateMessage(message.id, message.toEntity().toApiRequest())
                if (response.isSuccessful) {
                    response.body()?.let { messageResponse ->
                        val entity = messageResponse.toEntity()
                        messageDao.updateMessage(entity)
                        Result.success(entity.toDomainModel())
                    } ?: Result.failure(IOException("Empty response from server"))
                } else {
                    val entity = message.toEntity().copy(syncStatus = "pending_update")
                    messageDao.updateMessage(entity)
                    Result.success(entity.toDomainModel())
                }
            } else {
                val entity = message.toEntity().copy(syncStatus = "pending_update")
                messageDao.updateMessage(entity)
                Result.success(entity.toDomainModel())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMessage(messageId: String): Result<Unit> {
        return try {
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.deleteMessage(messageId)
                if (response.isSuccessful) {
                    messageDao.deleteMessage(messageId)
                    Result.success(Unit)
                } else {
                    messageDao.markMessageForDeletion(messageId, System.currentTimeMillis())
                    Result.success(Unit)
                }
            } else {
                messageDao.markMessageForDeletion(messageId, System.currentTimeMillis())
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markMessageAsRead(messageId: String): Result<Unit> {
        return try {
            val userId = dataStoreManager.getCurrentUserId()
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.markMessageAsRead(messageId, userId)
                if (response.isSuccessful) {
                    val readStatus = MessageReadStatus(messageId, userId, System.currentTimeMillis())
                    messageReadStatusDao.insertReadStatus(readStatus.toEntity())
                    Result.success(Unit)
                } else {
                    val readStatus = MessageReadStatus(messageId, userId, System.currentTimeMillis(), "pending")
                    messageReadStatusDao.insertReadStatus(readStatus.toEntity())
                    Result.success(Unit)
                }
            } else {
                val readStatus = MessageReadStatus(messageId, userId, System.currentTimeMillis(), "pending")
                messageReadStatusDao.insertReadStatus(readStatus.toEntity())
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addReaction(messageId: String, userId: String, reaction: String): Result<Unit> {
        return try {
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.addReaction(messageId, userId, reaction)
                if (response.isSuccessful) {
                    val messageReaction = MessageReaction(messageId, userId, reaction, System.currentTimeMillis())
                    messageReactionDao.insertReaction(messageReaction.toEntity())
                    Result.success(Unit)
                } else {
                    val messageReaction = MessageReaction(messageId, userId, reaction, System.currentTimeMillis(), "pending")
                    messageReactionDao.insertReaction(messageReaction.toEntity())
                    Result.success(Unit)
                }
            } else {
                val messageReaction = MessageReaction(messageId, userId, reaction, System.currentTimeMillis(), "pending")
                messageReactionDao.insertReaction(messageReaction.toEntity())
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeReaction(messageId: String, userId: String, reaction: String): Result<Unit> {
        return try {
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.removeReaction(messageId, userId, reaction)
                if (response.isSuccessful) {
                    messageReactionDao.deleteReaction(messageId)
                    Result.success(Unit)
                } else {
                    messageReactionDao.markReactionForDeletion(messageId, System.currentTimeMillis())
                    Result.success(Unit)
                }
            } else {
                messageReactionDao.markReactionForDeletion(messageId, System.currentTimeMillis())
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncMessages(): Result<Unit> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                return Result.failure(IOException("No network connection"))
            }

            val pendingMessages = messageDao.getPendingSyncMessages()

            for (message in pendingMessages) {
                when (message.syncStatus) {
                    "pending_create" -> {
                        val response = apiService.createMessage(message.toApiRequest())
                        if (response.isSuccessful) {
                            response.body()?.let { messageResponse ->
                                val entity = messageResponse.toEntity()
                                messageDao.updateMessage(entity)
                            }
                        }
                    }
                    "pending_update" -> {
                        val response = apiService.updateMessage(message.id, message.toApiRequest())
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

            val pendingReadStatuses = messageReadStatusDao.getPendingSyncReadStatuses()
            for (readStatus in pendingReadStatuses) {
                val response = apiService.markMessageAsRead(readStatus.messageId, readStatus.userId)
                if (response.isSuccessful) {
                    messageReadStatusDao.markReadStatusAsSynced(readStatus.id)
                }
            }

            val pendingReactions = messageReactionDao.getPendingSyncReactions()
            for (reaction in pendingReactions) {
                when (reaction.syncStatus) {
                    "pending_create" -> {
                        val response = apiService.addReaction(reaction.messageId, reaction.userId, reaction.reaction)
                        if (response.isSuccessful) {
                            messageReactionDao.markReactionAsSynced(reaction.id)
                        }
                    }
                    "pending_delete" -> {
                        val response = apiService.removeReaction(reaction.messageId, reaction.userId, reaction.reaction)
                        if (response.isSuccessful) {
                            messageReactionDao.deleteReaction(reaction.id)
                        }
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
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