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
import com.example.taskapplication.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
    private val context: Context
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
}