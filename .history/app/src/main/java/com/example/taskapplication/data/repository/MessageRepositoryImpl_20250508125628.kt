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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
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
            val pendingMessages = messageDao.getPendingMessagesSync()
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
                        val existingMessage = messageDao.getMessageByServerIdSync(messageResponse.id)
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
            val pendingMessages = messageDao.getPendingTeamMessagesSync(teamId)
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
                        val existingMessage = messageDao.getMessageByServerIdSync(messageResponse.id)
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
                )
                messageReactionDao.insertReaction(entity)
                Result.success(entity.toDomainModel())
            } else {
                val request = MessageReaction(
                    id = java.util.UUID.randomUUID().toString(),
                    messageId = messageId,
                    reaction = reaction
                ).toEntity().toApiRequest()
                val response = apiService.addReaction(request)
                val entity = response.toEntity()
                messageReactionDao.insertReaction(entity)
                Result.success(entity.toDomainModel())
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
                apiService.removeReaction(reactionId)
                messageReactionDao.deleteReaction(reactionId)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveReadStatus(readStatus: com.example.taskapplication.domain.model.MessageReadStatus) {
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

    override fun getMessages(teamId: String): Flow<List<Message>> = flow {
        emit(messageDao.getMessages(teamId).map { it.toDomainModel() })
    }.flowOn(Dispatchers.IO)

    override fun getMessagesSync(teamId: String): List<Message> {
        return messageDao.getMessagesSync(teamId).map { it.toDomainModel() }
    }

    override fun getMessageById(id: String): Flow<Message?> = flow {
        emit(messageDao.getMessageById(id)?.toDomainModel())
    }.flowOn(Dispatchers.IO)

    override fun getMessageByIdSync(id: String): Message? {
        return messageDao.getMessageByIdSync(id)?.toDomainModel()
    }

    override fun getMessageByServerId(serverId: String): Flow<Message?> = flow {
        emit(messageDao.getMessageByServerId(serverId)?.toDomainModel())
    }.flowOn(Dispatchers.IO)

    override fun getMessageByServerIdSync(serverId: String): Message? {
        return messageDao.getMessageByServerIdSync(serverId)?.toDomainModel()
    }

    override suspend fun createMessage(message: Message): Result<Message> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                val entity = message.toEntity()
                entity.syncStatus = "pending"
                messageDao.insertMessage(entity)
                Result.success(entity.toDomainModel())
            } else {
                val response = apiService.createMessage(message.toApiRequest())
                val entity = response.toEntity()
                entity.syncStatus = "synced"
                messageDao.insertMessage(entity)
                Result.success(entity.toDomainModel())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getReactions(messageId: String): Flow<List<MessageReaction>> = flow {
        emit(messageReactionDao.getReactions(messageId).map { it.toDomainModel() })
    }.flowOn(Dispatchers.IO)

    override fun getReactionsSync(messageId: String): List<MessageReaction> {
        return messageReactionDao.getReactionsSync(messageId).map { it.toDomainModel() }
    }

    override fun getReactionById(id: String): Flow<MessageReaction?> = flow {
        emit(messageReactionDao.getReactionById(id)?.toDomainModel())
    }.flowOn(Dispatchers.IO)

    override fun getReactionByIdSync(id: String): MessageReaction? {
        return messageReactionDao.getReactionByIdSync(id)?.toDomainModel()
    }

    override fun getReactionByServerId(serverId: String): Flow<MessageReaction?> = flow {
        emit(messageReactionDao.getReactionByServerId(serverId)?.toDomainModel())
    }.flowOn(Dispatchers.IO)

    override fun getReactionByServerIdSync(serverId: String): MessageReaction? {
        return messageReactionDao.getReactionByServerIdSync(serverId)?.toDomainModel()
    }

    override suspend fun createReaction(reaction: MessageReaction) {
        withContext(Dispatchers.IO) {
            val entity = reaction.toEntity()
            messageReactionDao.insertReaction(entity)

            if (networkUtils.isNetworkAvailable()) {
                try {
                    val response = apiService.createReaction(entity.toApiRequest())
                    val updatedEntity = entity.copy(
                        serverId = response.id.toString(),
                        syncStatus = "synced"
                    )
                    messageReactionDao.updateReaction(updatedEntity)
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }

    override suspend fun updateReaction(reaction: MessageReaction) {
        withContext(Dispatchers.IO) {
            val entity = reaction.toEntity()
            messageReactionDao.updateReaction(entity)

            if (networkUtils.isNetworkAvailable()) {
                try {
                    apiService.updateReaction(entity.serverId!!, entity.toApiRequest())
                    val updatedEntity = entity.copy(syncStatus = "synced")
                    messageReactionDao.updateReaction(updatedEntity)
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }

    override suspend fun deleteReaction(reaction: MessageReaction) {
        withContext(Dispatchers.IO) {
            val entity = reaction.toEntity()
            messageReactionDao.deleteReaction(entity)

            if (networkUtils.isNetworkAvailable() && entity.serverId != null) {
                try {
                    apiService.deleteReaction(entity.serverId!!)
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }

    override fun getReadStatuses(messageId: String): Flow<List<MessageReadStatus>> = flow {
        emit(messageReadStatusDao.getReadStatuses(messageId).map { it.toDomainModel() })
    }.flowOn(Dispatchers.IO)

    override fun getReadStatusesSync(messageId: String): List<MessageReadStatus> {
        return messageReadStatusDao.getReadStatusesSync(messageId).map { it.toDomainModel() }
    }

    override fun getReadStatusById(id: String): Flow<MessageReadStatus?> = flow {
        emit(messageReadStatusDao.getReadStatusById(id)?.toDomainModel())
    }.flowOn(Dispatchers.IO)

    override fun getReadStatusByIdSync(id: String): MessageReadStatus? {
        return messageReadStatusDao.getReadStatusByIdSync(id)?.toDomainModel()
    }

    override fun getReadStatusByServerId(serverId: String): Flow<MessageReadStatus?> = flow {
        emit(messageReadStatusDao.getReadStatusByServerId(serverId)?.toDomainModel())
    }.flowOn(Dispatchers.IO)

    override fun getReadStatusByServerIdSync(serverId: String): MessageReadStatus? {
        return messageReadStatusDao.getReadStatusByServerIdSync(serverId)?.toDomainModel()
    }

    override suspend fun createReadStatus(readStatus: MessageReadStatus) {
        withContext(Dispatchers.IO) {
            val entity = readStatus.toEntity()
            messageReadStatusDao.insertReadStatus(entity)

            if (networkUtils.isNetworkAvailable()) {
                try {
                    val response = apiService.createReadStatus(entity.toApiRequest())
                    val updatedEntity = entity.copy(
                        serverId = response.id.toString(),
                        syncStatus = "synced"
                    )
                    messageReadStatusDao.updateReadStatus(updatedEntity)
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }

    override suspend fun updateReadStatus(readStatus: MessageReadStatus) {
        withContext(Dispatchers.IO) {
            val entity = readStatus.toEntity()
            messageReadStatusDao.updateReadStatus(entity)

            if (networkUtils.isNetworkAvailable()) {
                try {
                    apiService.updateReadStatus(entity.serverId!!, entity.toApiRequest())
                    val updatedEntity = entity.copy(syncStatus = "synced")
                    messageReadStatusDao.updateReadStatus(updatedEntity)
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }

    override suspend fun deleteReadStatus(readStatus: MessageReadStatus) {
        withContext(Dispatchers.IO) {
            val entity = readStatus.toEntity()
            messageReadStatusDao.deleteReadStatus(entity)

            if (networkUtils.isNetworkAvailable() && entity.serverId != null) {
                try {
                    apiService.deleteReadStatus(entity.serverId!!)
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }

    override suspend fun syncReadStatuses() {
        withContext(Dispatchers.IO) {
            if (!networkUtils.isNetworkAvailable()) return@withContext

            // Sync pending read statuses
            val pendingReadStatuses = messageReadStatusDao.getPendingReadStatusesSync()
            for (readStatus in pendingReadStatuses) {
                try {
                    when (readStatus.syncStatus) {
                        "pending" -> {
                            val response = apiService.createReadStatus(readStatus.toApiRequest())
                            val updatedReadStatus = readStatus.copy(
                                serverId = response.id.toString(),
                                syncStatus = "synced"
                            )
                            messageReadStatusDao.updateReadStatus(updatedReadStatus)
                        }
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }

            // Sync from server
            try {
                val serverReadStatuses = apiService.getReadStatuses()
                for (serverReadStatus in serverReadStatuses) {
                    val existingReadStatus = messageReadStatusDao.getReadStatusByServerIdSync(serverReadStatus.id.toString())
                    if (existingReadStatus == null) {
                        messageReadStatusDao.insertReadStatus(serverReadStatus.toEntity())
                    } else if (serverReadStatus.updatedAt > existingReadStatus.lastModified) {
                        messageReadStatusDao.updateReadStatus(serverReadStatus.toEntity(existingReadStatus))
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    override suspend fun syncReadStatusesByMessage(messageId: String) {
        withContext(Dispatchers.IO) {
            if (!networkUtils.isNetworkAvailable()) return@withContext

            // Sync pending read statuses for message
            val pendingReadStatuses = messageReadStatusDao.getPendingReadStatusesByMessageSync(messageId)
            for (readStatus in pendingReadStatuses) {
                try {
                    when (readStatus.syncStatus) {
                        "pending" -> {
                            val response = apiService.createReadStatus(readStatus.toApiRequest())
                            val updatedReadStatus = readStatus.copy(
                                serverId = response.id.toString(),
                                syncStatus = "synced"
                            )
                            messageReadStatusDao.updateReadStatus(updatedReadStatus)
                        }
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }

            // Sync from server for message
            try {
                val serverReadStatuses = apiService.getReadStatusesByMessage(messageId)
                for (serverReadStatus in serverReadStatuses) {
                    val existingReadStatus = messageReadStatusDao.getReadStatusByServerIdSync(serverReadStatus.id.toString())
                    if (existingReadStatus == null) {
                        messageReadStatusDao.insertReadStatus(serverReadStatus.toEntity())
                    } else if (serverReadStatus.updatedAt > existingReadStatus.lastModified) {
                        messageReadStatusDao.updateReadStatus(serverReadStatus.toEntity(existingReadStatus))
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}