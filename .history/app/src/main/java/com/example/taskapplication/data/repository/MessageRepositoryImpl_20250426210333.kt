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

    override fun getTeamMessages(teamId: String): Flow<List<Message>> {
        return messageDao.getMessagesByTeamId(teamId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getTaskMessages(taskId: String): Flow<List<Message>> {
        return messageDao.getMessagesByTaskId(taskId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getMessage(messageId: String): Flow<Message?> {
        return messageDao.getMessageById(messageId).map { it?.toDomainModel() }
    }

    override suspend fun sendMessage(message: Message): Result<Message> {
        val messageWithId = message.copy(
            id = UUID.randomUUID().toString(),
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

        if (message.serverId == null) {
            // If the message has never been synced, just delete it locally
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
                    val response = when {
                        message.teamId != null -> apiService.sendTeamMessage(
                            message.teamId, 
                            message.toApiRequest()
                        )
                        message.taskId != null -> apiService.sendTaskMessage(
                            message.taskId, 
                            message.toApiRequest()
                        )
                        else -> {
                            Log.e(TAG, "Message has neither teamId nor taskId: ${message.id}")
                            continue
                        }
                    }
                    
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
                if (message.serverId == null) continue
                
                try {
                    val response = when {
                        message.teamId != null -> apiService.deleteTeamMessage(
                            message.teamId, 
                            message.serverId
                        )
                        message.taskId != null -> apiService.deleteTaskMessage(
                            message.taskId, 
                            message.serverId
                        )
                        else -> {
                            Log.e(TAG, "Message has neither teamId nor taskId: ${message.id}")
                            continue
                        }
                    }
                    
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
                
                // Fetch team messages
                fetchTeamMessages(lastSyncTimestamp)
                
                // Fetch task messages
                fetchTaskMessages(lastSyncTimestamp)
                
                // Update last sync timestamp
                dataStoreManager.saveLastMessageSyncTimestamp(System.currentTimeMillis())
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching remote messages", e)
            }
            
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error during message sync", e)
            return Result.failure(e)
        }
    }
    
    private suspend fun fetchTeamMessages(lastSyncTimestamp: Long) {
        try {
            val response = apiService.getTeamMessages(lastSyncTimestamp)
            
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
            } else {
                Log.e(TAG, "Failed to fetch remote team messages: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching remote team messages", e)
        }
    }
    
    private suspend fun fetchTaskMessages(lastSyncTimestamp: Long) {
        try {
            val response = apiService.getTaskMessages(lastSyncTimestamp)
            
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
            } else {
                Log.e(TAG, "Failed to fetch remote task messages: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
    private suspend fun processRemoteMessages(remoteMessages: List<Message>) {
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
    }
} 