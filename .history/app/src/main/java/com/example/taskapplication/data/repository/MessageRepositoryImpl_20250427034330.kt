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
        return messageDao.getTeamMessages(teamId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getDirectMessages(userId1: String, userId2: String): Flow<List<Message>> {
        return messageDao.getDirectMessages(userId1, userId2).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getMessageById(id: String): Message? {
        return messageDao.getMessageById(id)?.toDomainModel()
    }

    override suspend fun sendTeamMessage(teamId: String, content: String): Result<Message> {
        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        // Check if team exists
        val team = teamDao.getTeamByIdSync(teamId)
            ?: return Result.failure(NoSuchElementException("Team not found"))

        // Check if user is member of the team
        val isMember = teamMemberDao.isUserMemberOfTeam(teamId, currentUserId)
        if (!isMember) {
            return Result.failure(IllegalStateException("You are not a member of this team"))
        }

        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val message = Message(
            id = messageId,
            teamId = teamId,
            senderId = currentUserId,
            receiverId = null,
            content = content,
            timestamp = timestamp,
            isRead = false,
            isDeleted = false,
            isEdited = false,
            syncStatus = "pending_create",
            lastModified = timestamp,
            serverId = null
        )

        messageDao.insertMessage(message.toEntity())

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncMessages()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after sending message", e)
            }
        }

        return Result.success(message)
    }

    override suspend fun deleteMessage(messageId: String): Result<Unit> {
        val message = messageDao.getMessageByIdSync(messageId)
            ?: return Result.failure(NoSuchElementException("Message not found"))

        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        // Check if user is the sender or team admin
        if (message.senderId != currentUserId) {
            val isAdmin = teamMemberDao.isUserAdminOfTeam(message.teamId, currentUserId)
            if (!isAdmin) {
                return Result.failure(IllegalStateException("Only the sender or team admin can delete this message"))
            }
        }

        if (message.serverId == null) {
            // If message has never been synced, just delete locally
            messageDao.deleteMessage(messageId)
        } else {
            // Mark for deletion during next sync
            messageDao.markMessageForDeletion(messageId, System.currentTimeMillis())
        }

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncMessages()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after deleting message", e)
            }
        }

        return Result.success(Unit)
    }

    override suspend fun updateMessage(message: Message): Result<Message> {
        val existingMessage = messageDao.getMessageByIdSync(message.id)
            ?: return Result.failure(NoSuchElementException("Message not found"))

        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        // Only the sender can edit the message
        if (existingMessage.senderId != currentUserId) {
            return Result.failure(IllegalStateException("Only the sender can edit this message"))
        }

        // Can't edit the core message properties, only the content
        val updatedMessage = existingMessage.copy(
            content = message.content,
            isEdited = true,
            syncStatus = if (existingMessage.serverId == null) "pending_create" else "pending_update",
            lastModified = System.currentTimeMillis()
        )

        messageDao.updateMessage(updatedMessage)

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncMessages()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after editing message", e)
            }
        }

        return Result.success(updatedMessage.toDomainModel())
    }

    override suspend fun markMessageAsRead(messageId: String): Result<Unit> {
        val message = messageDao.getMessageByIdSync(messageId)
            ?: return Result.failure(NoSuchElementException("Message not found"))

        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        // Check if user is member of the team
        if (message.teamId != null) {
            val isMember = teamMemberDao.isUserMemberOfTeam(message.teamId, currentUserId)
            if (!isMember) {
                return Result.failure(IllegalStateException("You are not a member of this team"))
            }
        }

        // Don't mark own messages as read
        if (message.senderId == currentUserId) {
            return Result.success(Unit)
        }

        // Create a read status entity
        val readStatus = com.example.taskapplication.data.database.entities.MessageReadStatusEntity(
            id = UUID.randomUUID().toString(),
            messageId = messageId,
            userId = currentUserId,
            readAt = System.currentTimeMillis(),
            serverId = null,
            syncStatus = "pending_create",
            lastModified = System.currentTimeMillis()
        )

        // Save the read status
        messageReadStatusDao.insertReadStatus(readStatus)

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncMessages()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after marking message as read", e)
            }
        }

        return Result.success(Unit)
    }

    override suspend fun getUnreadMessageCount(): Result<Map<String, Int>> {
        val currentUserId = dataStoreManager.getCurrentUserId()
            ?: return Result.failure(IllegalStateException("No current user found"))

        try {
            val teams = teamDao.getAllTeams().first()
            val result = mutableMapOf<String, Int>()

            for (team in teams) {
                val count = messageDao.getUnreadMessagesCount(team.id, currentUserId).first()
                result[team.id] = count
            }

            return Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting unread message count", e)
            return Result.failure(e)
        }
    }

    override suspend fun syncMessages(): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        try {
            val pendingMessages = messageDao.getPendingSyncMessages()

            // Group by sync status
            val messagesToCreate = pendingMessages.filter { it.syncStatus == "pending_create" }
            val messagesToUpdate = pendingMessages.filter { it.syncStatus == "pending_update" }
            val messagesToDelete = pendingMessages.filter { it.syncStatus == "pending_delete" }

            // Process creates
            for (message in messagesToCreate) {
                try {
                    val team = teamDao.getTeamByIdSync(message.teamId)
                        ?: continue // Skip if team not found

                    if (team.serverId == null) {
                        continue // Skip if team is not synced yet
                    }

                    // Use server id for the team
                    val messageRequest = message.toApiRequest()
                    val response = apiService.sendTeamMessage(team.serverId!!.toLong(), messageRequest)

                    if (response.isSuccessful && response.body() != null) {
                        val serverMessage = response.body()!!
                        messageDao.updateMessage(
                            message.copy(
                                serverId = serverMessage.id,
                                syncStatus = "synced",
                                lastModified = System.currentTimeMillis()
                            )
                        )
                    } else {
                        Log.e(TAG, "Failed to create message: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating message", e)
                    continue
                }
            }

            // Process updates
            for (message in messagesToUpdate) {
                try {
                    if (message.serverId == null) {
                        continue // Skip if message is not synced yet
                    }

                    val team = teamDao.getTeamByIdSync(message.teamId)
                        ?: continue // Skip if team not found

                    if (team.serverId == null) {
                        continue // Skip if team is not synced yet
                    }

                    // Use server id for the team
                    val messageRequest = message.toApiRequest().copy(teamId = team.serverId)
                    val response = apiService.updateMessage(message.serverId, messageRequest)

                    if (response.isSuccessful) {
                        messageDao.updateMessage(
                            message.copy(
                                syncStatus = "synced",
                                lastModified = System.currentTimeMillis()
                            )
                        )
                    } else {
                        Log.e(TAG, "Failed to update message: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating message", e)
                    continue
                }
            }

            // Process deletes
            for (message in messagesToDelete) {
                if (message.serverId == null) {
                    // If message was never synced, we can just delete it locally
                    messageDao.deleteMessage(message.id)
                    continue
                }

                try {
                    val response = apiService.deleteMessage(message.serverId)

                    if (response.isSuccessful) {
                        messageDao.deleteMessage(message.id)
                    } else {
                        Log.e(TAG, "Failed to delete message: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting message", e)
                    continue
                }
            }

            // Fetch and merge remote messages
            try {
                val lastSyncTimestamp = dataStoreManager.getLastMessageSyncTimestamp() ?: 0
                val response = apiService.getMessages(lastSyncTimestamp)

                if (response.isSuccessful && response.body() != null) {
                    val remoteMessages = response.body()!!

                    for (remoteMessage in remoteMessages) {
                        // Find local team by server ID
                        val localTeam = teamDao.getTeamByServerIdSync(remoteMessage.teamId)
                            ?: continue // Skip if team doesn't exist locally

                        val localMessage = messageDao.getMessageByServerIdSync(remoteMessage.id)

                        if (localMessage == null) {
                            // New message from server
                            messageDao.insertMessage(
                                remoteMessage.toEntity().copy(
                                    id = UUID.randomUUID().toString(),
                                    teamId = localTeam.id, // Use local team ID
                                    syncStatus = "synced",
                                    serverId = remoteMessage.id
                                )
                            )
                        } else if (remoteMessage.lastModified > localMessage.lastModified &&
                                  localMessage.syncStatus != "pending_update" &&
                                  localMessage.syncStatus != "pending_delete") {
                            // Server has newer version and we're not in the middle of an update or delete
                            messageDao.updateMessage(
                                remoteMessage.toEntity().copy(
                                    id = localMessage.id,
                                    teamId = localTeam.id, // Use local team ID
                                    syncStatus = "synced",
                                    serverId = remoteMessage.id
                                )
                            )
                        }
                    }

                    // Update last sync timestamp
                    dataStoreManager.saveLastMessageSyncTimestamp(System.currentTimeMillis())
                } else {
                    Log.e(TAG, "Failed to fetch remote messages: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching remote messages", e)
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error during message sync", e)
            return Result.failure(e)
        }
    }

    override suspend fun sendDirectMessage(receiverId: String, content: String): Result<Message> {
        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val message = Message(
            id = messageId,
            teamId = null,
            senderId = currentUserId,
            receiverId = receiverId,
            content = content,
            timestamp = timestamp,
            isRead = false,
            isDeleted = false,
            isEdited = false,
            syncStatus = "pending_create",
            lastModified = timestamp,
            serverId = null
        )

        messageDao.insertMessage(message.toEntity())

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncMessages()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after sending direct message", e)
            }
        }

        return Result.success(message)
    }

    override suspend fun syncTeamMessages(teamId: String): Result<Unit> {
        // Similar to syncMessages but filtered by teamId
        return Result.success(Unit)
    }

    // WebSocket support
    override suspend fun saveMessage(message: Message) {
        messageDao.insertMessage(message.toEntity())
    }

    override suspend fun addReaction(messageId: String, reaction: String): Result<MessageReaction> {
        val message = messageDao.getMessageByIdSync(messageId)
            ?: return Result.failure(NoSuchElementException("Message not found"))

        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        val reactionId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val messageReaction = com.example.taskapplication.domain.model.MessageReaction(
            id = reactionId,
            messageId = messageId,
            userId = currentUserId,
            reaction = reaction,
            timestamp = timestamp,
            syncStatus = "pending_create",
            serverId = null
        )

        // Convert domain model to entity
        val entity = com.example.taskapplication.data.database.entities.MessageReactionEntity(
            id = messageReaction.id,
            messageId = messageReaction.messageId,
            userId = messageReaction.userId,
            reaction = messageReaction.reaction,
            serverId = messageReaction.serverId,
            syncStatus = messageReaction.syncStatus,
            lastModified = messageReaction.lastModified
        )

        messageReactionDao.insertReaction(entity)

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncMessages()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after adding reaction", e)
            }
        }

        return Result.success(messageReaction)
    }

    override suspend fun removeReaction(messageId: String, reactionId: String): Result<Unit> {
        // Get the reaction from the database
        val reactionEntity = messageReactionDao.getReactionsByMessage(messageId).first()
            .find { it.id == reactionId }
            ?: return Result.failure(NoSuchElementException("Reaction not found"))

        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        // Only the user who added the reaction can remove it
        if (reactionEntity.userId != currentUserId) {
            return Result.failure(IllegalStateException("Only the user who added the reaction can remove it"))
        }

        messageReactionDao.deleteReactionById(reactionId)

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncMessages()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after removing reaction", e)
            }
        }

        return Result.success(Unit)
    }

    override suspend fun saveReadStatus(readStatus: com.example.taskapplication.domain.model.MessageReadStatus) {
        // Convert domain model to entity and save
        val entity = com.example.taskapplication.data.database.entities.MessageReadStatusEntity(
            id = UUID.randomUUID().toString(),
            messageId = readStatus.messageId,
            userId = readStatus.userId,
            readAt = readStatus.readAt,
            serverId = readStatus.serverId,
            syncStatus = readStatus.syncStatus,
            lastModified = readStatus.lastModified
        )
        messageReadStatusDao.insertReadStatus(entity)
    }

    override suspend fun getOlderTeamMessages(teamId: String, olderThan: Long, limit: Int): Result<List<Message>> {
        try {
            val messages = messageDao.getOlderTeamMessages(teamId, olderThan, limit)
            return Result.success(messages.map { it.toDomainModel() })
        } catch (e: Exception) {
            Log.e(TAG, "Error getting older team messages", e)
            return Result.failure(e)
        }
    }

    override suspend fun markMessageAsDeleted(messageId: Long) {
        messageDao.markMessageAsDeleted(messageId, System.currentTimeMillis())
    }
}