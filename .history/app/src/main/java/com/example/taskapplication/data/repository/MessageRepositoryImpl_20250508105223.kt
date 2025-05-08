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
        return messageDao.getTeamMessages(teamId).map { messages ->
            messages.map { it.toDomainModel() }
        }
    }

    override fun getDirectMessages(userId1: String, userId2: String): Flow<List<Message>> {
        return messageDao.getDirectMessages(userId1, userId2).map { messages ->
            messages.map { it.toDomainModel() }
        }
    }

    override suspend fun getMessageById(id: String): Message? {
        return messageDao.getMessage(id)?.toDomainModel()
    }

    override suspend fun sendTeamMessage(teamId: String, content: String): Result<Message> {
        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        val existingTeam = teamDao.getTeamSync(teamId) ?:
            return Result.failure(IllegalStateException("Team not found"))

        if (!teamMemberDao.isUserMemberOfTeam(teamId, currentUserId)) {
            return Result.failure(IllegalStateException("You are not a member of this team"))
        }

        val message = Message(
            id = UUID.randomUUID().toString(),
            teamId = teamId,
            senderId = currentUserId,
            receiverId = null,
            content = content,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            isDeleted = false,
            syncStatus = "pending",
            lastModified = System.currentTimeMillis(),
            serverId = null
        )

        return try {
            messageDao.insertMessage(message.toEntity())
            Result.success(message)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending team message", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteMessage(messageId: String): Result<Unit> {
        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        val existingMessage = messageDao.getMessageSync(messageId) ?:
            return Result.failure(IllegalStateException("Message not found"))

        if (existingMessage.senderId != currentUserId) {
            val isAdmin = teamMemberDao.isUserAdminOfTeam(existingMessage.teamId ?: "", currentUserId)
            if (!isAdmin) {
                return Result.failure(IllegalStateException("Only the sender or team admin can delete this message"))
            }
        }

        return try {
            messageDao.deleteMessage(messageId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting message", e)
            Result.failure(e)
        }
    }

    override suspend fun updateMessage(message: Message): Result<Message> {
        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        val existingMessage = messageDao.getMessageSync(message.id) ?:
            return Result.failure(IllegalStateException("Message not found"))

        if (existingMessage.senderId != currentUserId) {
            return Result.failure(IllegalStateException("Only the sender can edit this message"))
        }

        val updatedMessage = message.copy(
            syncStatus = "pending",
            lastModified = System.currentTimeMillis()
        )

        return try {
            messageDao.updateMessage(updatedMessage.toEntity())
            Result.success(updatedMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating message", e)
            Result.failure(e)
        }
    }

    override suspend fun markMessageAsRead(messageId: String): Result<Unit> {
        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        val existingMessage = messageDao.getMessageSync(messageId) ?:
            return Result.failure(IllegalStateException("Message not found"))

        if (existingMessage.teamId != null) {
            if (!teamMemberDao.isUserMemberOfTeam(existingMessage.teamId, currentUserId)) {
                return Result.failure(IllegalStateException("You are not a member of this team"))
            }
        }

        if (existingMessage.senderId == currentUserId) {
            return Result.success(Unit)
        }

        val readStatus = com.example.taskapplication.domain.model.MessageReadStatus(
            id = UUID.randomUUID().toString(),
            messageId = messageId,
            userId = currentUserId,
            readAt = System.currentTimeMillis(),
            serverId = null,
            syncStatus = "pending",
            lastModified = System.currentTimeMillis()
        )

        return try {
            messageReadStatusDao.insertReadStatus(readStatus.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking message as read", e)
            Result.failure(e)
        }
    }

    override suspend fun getUnreadMessageCount(): Result<Map<String, Int>> {
        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        return try {
            val teams = teamDao.getAllTeams().first()
            val result = mutableMapOf<String, Int>()

            for (team in teams) {
                val count = messageDao.getUnreadMessagesCount(team.id, currentUserId).first()
                result[team.id] = count
            }

            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting unread message count", e)
            Result.failure(e)
        }
    }

    override suspend fun syncMessages(): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        return try {
            val pendingMessages = messageDao.getPendingMessages()
            for (message in pendingMessages) {
                when (message.syncStatus) {
                    "pending_create" -> {
                        val response = apiService.createMessage(message.toApiRequest())
                        if (response.isSuccessful) {
                            val serverId = response.body()?.id.toString()
                            messageDao.updateMessageServerId(message.id, serverId)
                            messageDao.markMessageAsSynced(message.id)
                        }
                    }
                    "pending_update" -> {
                        val response = apiService.updateMessage(message.serverId!!, message.toApiRequest())
                        if (response.isSuccessful) {
                            messageDao.markMessageAsSynced(message.id)
                        }
                    }
                    "pending_delete" -> {
                        val response = apiService.deleteMessage(message.serverId!!)
                        if (response.isSuccessful) {
                            messageDao.deleteMessage(message.id)
                        }
                    }
                }
            }

            val response = apiService.getMessages()
            if (response.isSuccessful) {
                val serverMessages = response.body() ?: emptyList()
                for (serverMessage in serverMessages) {
                    val existingMessage = messageDao.getMessageByServerId(serverMessage.id.toString())
                    if (existingMessage == null) {
                        messageDao.insertMessage(serverMessage.toEntity())
                    } else {
                        messageDao.updateMessage(serverMessage.toEntity(existingMessage))
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing messages", e)
            Result.failure(e)
        }
    }

    override suspend fun sendDirectMessage(receiverId: String, content: String): Result<Message> {
        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        val message = Message(
            id = UUID.randomUUID().toString(),
            teamId = null,
            senderId = currentUserId,
            receiverId = receiverId,
            content = content,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            isDeleted = false,
            syncStatus = "pending",
            lastModified = System.currentTimeMillis(),
            serverId = null
        )

        return try {
            messageDao.insertMessage(message.toEntity())
            Result.success(message)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending direct message", e)
            Result.failure(e)
        }
    }

    override suspend fun syncTeamMessages(teamId: String): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        return try {
            val response = apiService.getTeamMessages(teamId)
            if (response.isSuccessful) {
                val serverMessages = response.body() ?: emptyList()
                for (serverMessage in serverMessages) {
                    val existingMessage = messageDao.getMessageByServerId(serverMessage.id.toString())
                    if (existingMessage == null) {
                        messageDao.insertMessage(serverMessage.toEntity())
                    } else {
                        messageDao.updateMessage(serverMessage.toEntity(existingMessage))
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing team messages", e)
            Result.failure(e)
        }
    }

    override suspend fun saveMessage(message: Message) {
        messageDao.insertMessage(message.toEntity())
    }

    override suspend fun addReaction(messageId: String, reaction: String): Result<MessageReaction> {
        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        val existingMessage = messageDao.getMessageSync(messageId) ?:
            return Result.failure(IllegalStateException("Message not found"))

        val messageReaction = MessageReaction(
            id = UUID.randomUUID().toString(),
            messageId = messageId,
            userId = currentUserId,
            reaction = reaction,
            timestamp = System.currentTimeMillis(),
            syncStatus = "pending",
            serverId = null,
            lastModified = System.currentTimeMillis()
        )

        return try {
            messageReactionDao.insertReaction(messageReaction.toEntity())
            Result.success(messageReaction)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding reaction", e)
            Result.failure(e)
        }
    }

    override suspend fun removeReaction(messageId: String, reactionId: String): Result<Unit> {
        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        val existingReaction = messageReactionDao.getReactionSync(reactionId) ?:
            return Result.failure(IllegalStateException("Reaction not found"))

        if (existingReaction.userId != currentUserId) {
            return Result.failure(IllegalStateException("Only the user who added the reaction can remove it"))
        }

        return try {
            messageReactionDao.deleteReaction(reactionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing reaction", e)
            Result.failure(e)
        }
    }

    override suspend fun saveReadStatus(readStatus: com.example.taskapplication.domain.model.MessageReadStatus) {
        messageReadStatusDao.insertReadStatus(readStatus.toEntity())
    }

    override suspend fun getOlderTeamMessages(teamId: String, olderThan: Long, limit: Int): Result<List<Message>> {
        return try {
            val messages = messageDao.getOlderTeamMessages(teamId, olderThan, limit)
            Result.success(messages.map { it.toDomainModel() })
        } catch (e: Exception) {
            Log.e(TAG, "Error getting older team messages", e)
            Result.failure(e)
        }
    }

    override suspend fun markMessageAsDeleted(messageId: String) {
        messageDao.markMessageAsDeleted(messageId, System.currentTimeMillis())
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

    override suspend fun createMessage(message: Message) {
        withContext(Dispatchers.IO) {
            val entity = message.toEntity()
            messageDao.insertMessage(entity)

            if (networkUtils.isNetworkAvailable()) {
                try {
                    val response = apiService.createMessage(entity.toApiRequest())
                    val updatedEntity = entity.copy(
                        serverId = response.id.toString(),
                        syncStatus = "synced"
                    )
                    messageDao.updateMessage(updatedEntity)
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }

    override suspend fun updateMessage(message: Message) {
        withContext(Dispatchers.IO) {
            val entity = message.toEntity()
            messageDao.updateMessage(entity)

            if (networkUtils.isNetworkAvailable()) {
                try {
                    apiService.updateMessage(entity.serverId!!, entity.toApiRequest())
                    val updatedEntity = entity.copy(syncStatus = "synced")
                    messageDao.updateMessage(updatedEntity)
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }

    override suspend fun deleteMessage(message: Message) {
        withContext(Dispatchers.IO) {
            val entity = message.toEntity()
            messageDao.deleteMessage(entity)

            if (networkUtils.isNetworkAvailable() && entity.serverId != null) {
                try {
                    apiService.deleteMessage(entity.serverId!!)
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }

    override suspend fun syncMessages() {
        withContext(Dispatchers.IO) {
            if (!networkUtils.isNetworkAvailable()) return@withContext

            // Sync pending messages
            val pendingMessages = messageDao.getPendingMessagesSync()
            for (message in pendingMessages) {
                try {
                    when (message.syncStatus) {
                        "pending" -> {
                            val response = apiService.createMessage(message.toApiRequest())
                            val updatedMessage = message.copy(
                                serverId = response.id.toString(),
                                syncStatus = "synced"
                            )
                            messageDao.updateMessage(updatedMessage)
                        }
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }

            // Sync from server
            try {
                val serverMessages = apiService.getMessages()
                for (serverMessage in serverMessages) {
                    val existingMessage = messageDao.getMessageByServerIdSync(serverMessage.id.toString())
                    if (existingMessage == null) {
                        messageDao.insertMessage(serverMessage.toEntity())
                    } else if (serverMessage.updatedAt > existingMessage.lastModified) {
                        messageDao.updateMessage(serverMessage.toEntity(existingMessage))
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    override suspend fun syncMessagesByTeam(teamId: String) {
        withContext(Dispatchers.IO) {
            if (!networkUtils.isNetworkAvailable()) return@withContext

            // Sync pending messages for team
            val pendingMessages = messageDao.getPendingMessagesByTeamSync(teamId)
            for (message in pendingMessages) {
                try {
                    when (message.syncStatus) {
                        "pending" -> {
                            val response = apiService.createMessage(message.toApiRequest())
                            val updatedMessage = message.copy(
                                serverId = response.id.toString(),
                                syncStatus = "synced"
                            )
                            messageDao.updateMessage(updatedMessage)
                        }
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }

            // Sync from server for team
            try {
                val serverMessages = apiService.getMessagesByTeam(teamId)
                for (serverMessage in serverMessages) {
                    val existingMessage = messageDao.getMessageByServerIdSync(serverMessage.id.toString())
                    if (existingMessage == null) {
                        messageDao.insertMessage(serverMessage.toEntity())
                    } else if (serverMessage.updatedAt > existingMessage.lastModified) {
                        messageDao.updateMessage(serverMessage.toEntity(existingMessage))
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
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

    override suspend fun syncReactions() {
        withContext(Dispatchers.IO) {
            if (!networkUtils.isNetworkAvailable()) return@withContext

            // Sync pending reactions
            val pendingReactions = messageReactionDao.getPendingReactionsSync()
            for (reaction in pendingReactions) {
                try {
                    when (reaction.syncStatus) {
                        "pending" -> {
                            val response = apiService.createReaction(reaction.toApiRequest())
                            val updatedReaction = reaction.copy(
                                serverId = response.id.toString(),
                                syncStatus = "synced"
                            )
                            messageReactionDao.updateReaction(updatedReaction)
                        }
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }

            // Sync from server
            try {
                val serverReactions = apiService.getReactions()
                for (serverReaction in serverReactions) {
                    val existingReaction = messageReactionDao.getReactionByServerIdSync(serverReaction.id.toString())
                    if (existingReaction == null) {
                        messageReactionDao.insertReaction(serverReaction.toEntity())
                    } else if (serverReaction.updatedAt > existingReaction.lastModified) {
                        messageReactionDao.updateReaction(serverReaction.toEntity(existingReaction))
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    override suspend fun syncReactionsByMessage(messageId: String) {
        withContext(Dispatchers.IO) {
            if (!networkUtils.isNetworkAvailable()) return@withContext

            // Sync pending reactions for message
            val pendingReactions = messageReactionDao.getPendingReactionsByMessageSync(messageId)
            for (reaction in pendingReactions) {
                try {
                    when (reaction.syncStatus) {
                        "pending" -> {
                            val response = apiService.createReaction(reaction.toApiRequest())
                            val updatedReaction = reaction.copy(
                                serverId = response.id.toString(),
                                syncStatus = "synced"
                            )
                            messageReactionDao.updateReaction(updatedReaction)
                        }
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }

            // Sync from server for message
            try {
                val serverReactions = apiService.getReactionsByMessage(messageId)
                for (serverReaction in serverReactions) {
                    val existingReaction = messageReactionDao.getReactionByServerIdSync(serverReaction.id.toString())
                    if (existingReaction == null) {
                        messageReactionDao.insertReaction(serverReaction.toEntity())
                    } else if (serverReaction.updatedAt > existingReaction.lastModified) {
                        messageReactionDao.updateReaction(serverReaction.toEntity(existingReaction))
                    }
                }
            } catch (e: Exception) {
                // Handle error
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