package com.example.taskapplication.data.repository

import android.util.Log
import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.database.dao.MessageDao
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
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager,
    private val connectionChecker: ConnectionChecker
) : MessageRepository {

    private val TAG = "MessageRepository"

    override fun getMessagesForTeam(teamId: String): Flow<List<Message>> {
        return messageDao.getMessagesByTeam(teamId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getMessagesForTask(taskId: String): Flow<List<Message>> {
        return messageDao.getMessagesByTask(taskId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun sendMessage(message: Message): Result<Message> {
        val messageEntity = message.copy(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            syncStatus = "pending_create",
            lastModified = System.currentTimeMillis()
        ).toEntity()

        messageDao.insertMessage(messageEntity)

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncMessages()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after sending message", e)
            }
        }

        return Result.success(messageEntity.toDomainModel())
    }

    override suspend fun editMessage(message: Message): Result<Message> {
        val existingMessage = messageDao.getMessageById(message.id)
            ?: return Result.failure(NoSuchElementException("Message not found"))

        val updatedEntity = message.copy(
            timestamp = existingMessage.timestamp,
            edited = true,
            syncStatus = if (existingMessage.serverId != null) "pending_update" else "pending_create",
            lastModified = System.currentTimeMillis(),
            serverId = existingMessage.serverId
        ).toEntity()

        messageDao.updateMessage(updatedEntity)

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncMessages()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after editing message", e)
            }
        }

        return Result.success(updatedEntity.toDomainModel())
    }

    override suspend fun deleteMessage(messageId: String): Result<Unit> {
        val message = messageDao.getMessageById(messageId)
            ?: return Result.failure(NoSuchElementException("Message not found"))

        if (message.serverId == null) {
            // If the message has never been synced, just delete it locally
            messageDao.deleteLocalOnlyMessage(messageId)
        } else {
            // Mark for deletion during next sync
            messageDao.markMessageForDeletion(messageId, System.currentTimeMillis())
        }

        // If connected, try to sync immediately
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncMessages()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after delete message", e)
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
            val messagesToUpdate = pendingMessages.filter { it.syncStatus == "pending_update" }
            val messagesToDelete = pendingMessages.filter { it.syncStatus == "pending_delete" }
            
            // Process creates
            for (message in messagesToCreate) {
                try {
                    val response = apiService.sendMessage(message.toApiRequest())
                    if (response.isSuccessful && response.body() != null) {
                        val serverMessage = response.body()!!
                        messageDao.insertMessage(
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
                if (message.serverId == null) continue
                
                try {
                    val response = apiService.updateMessage(message.serverId, message.toApiRequest())
                    if (response.isSuccessful) {
                        messageDao.insertMessage(
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
                if (message.serverId == null) continue
                
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
                        val localMessage = messageDao.getMessageByServerId(remoteMessage.id)
                        
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
                                  localMessage.syncStatus != "pending_update" && 
                                  localMessage.syncStatus != "pending_delete") {
                            // Server has newer version and we're not in the middle of an update
                            messageDao.insertMessage(
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