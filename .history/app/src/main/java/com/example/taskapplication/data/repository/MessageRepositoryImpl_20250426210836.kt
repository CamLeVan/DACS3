package com.example.taskapplication.data.repository

import android.util.Log
import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.database.dao.MessageDao
import com.example.taskapplication.data.database.dao.TeamMemberDao
import com.example.taskapplication.data.mapper.toApiRequest
import com.example.taskapplication.data.mapper.toDomainModel
import com.example.taskapplication.data.mapper.toEntity
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.domain.model.Message
import com.example.taskapplication.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val teamMemberDao: TeamMemberDao,
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager,
    private val connectionChecker: ConnectionChecker
) : MessageRepository {

    private val TAG = "MessageRepository"

    override fun getMessagesForTeam(teamId: String): Flow<List<Message>> {
        return messageDao.getMessagesByTeamId(teamId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun sendMessage(message: Message): Result<Message> {
        // Validate that the message belongs to a team
        if (message.teamId.isNullOrEmpty()) {
            return Result.failure(IllegalArgumentException("Message must have a teamId"))
        }

        // Validate that the current user is a member of the team
        val currentUserId = dataStoreManager.getCurrentUserId() ?: 
            return Result.failure(IllegalStateException("No current user found"))
            
        val isMember = teamMemberDao.isUserMemberOfTeam(message.teamId, currentUserId)
        if (!isMember) {
            return Result.failure(IllegalStateException("User is not a member of this team"))
        }

        val messageWithId = message.copy(
            id = UUID.randomUUID().toString(),
            senderId = currentUserId,
            timestamp = System.currentTimeMillis(),
            syncStatus = "pending_create",
            lastModified = System.currentTimeMillis()
        )
        
        messageDao.insertMessage(messageWithId.toEntity())
        
        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncMessages()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after sending message", e)
            }
        }
        
        return Result.success(messageWithId)
    }

    override suspend fun deleteMessage(messageId: String): Result<Unit> {
        val message = messageDao.getMessageByIdSync(messageId)
            ?: return Result.failure(NoSuchElementException("Message not found"))
        
        // Validate that the current user is the sender of the message
        val currentUserId = dataStoreManager.getCurrentUserId() ?: 
            return Result.failure(IllegalStateException("No current user found"))
            
        if (message.senderId != currentUserId) {
            return Result.failure(IllegalStateException("Only message sender can delete the message"))
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

    override suspend fun syncMessages(): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }
        
        try {
            val pendingMessages = messageDao.getPendingSyncMessages()
            
            // Group by sync status
            val messagesToCreate = pendingMessages.filter { it.syncStatus == "pending_create" }
            val messagesToDelete = pendingMessages.filter { it.syncStatus == "pending_delete" }
            
            // Process creates
            for (message in messagesToCreate) {
                try {
                    val response = apiService.sendMessage(message.toApiRequest())
                    
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
                        Log.e(TAG, "Failed to send message: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending message", e)
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
                        val localMessage = messageDao.getMessageByServerIdSync(remoteMessage.id)
                        
                        if (localMessage == null) {
                            // New message from server
                            messageDao.insertMessage(
                                remoteMessage.toEntity().copy(
                                    id = UUID.randomUUID().toString(),
                                    syncStatus = "synced",
                                    serverId = remoteMessage.id
                                )
                            )
                        } else if (remoteMessage.lastModified > localMessage.lastModified && 
                                  localMessage.syncStatus != "pending_delete") {
                            // Server has newer version and we're not in the middle of a delete
                            messageDao.updateMessage(
                                remoteMessage.toEntity().copy(
                                    id = localMessage.id,
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
} 