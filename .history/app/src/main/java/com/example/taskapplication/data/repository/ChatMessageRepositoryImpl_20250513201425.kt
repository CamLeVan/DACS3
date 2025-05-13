package com.example.taskapplication.data.repository

import android.util.Log
import com.example.taskapplication.data.api.ChatApiService
import com.example.taskapplication.data.api.model.ApiMessage
import com.example.taskapplication.data.api.model.ApiMessageDelete
import com.example.taskapplication.data.api.model.ApiMessageUpdate
import com.example.taskapplication.data.api.model.ApiReadStatus
import com.example.taskapplication.data.api.model.ApiReaction
import com.example.taskapplication.data.api.model.ApiTypingStatus
import com.example.taskapplication.data.api.model.EditMessageRequest
import com.example.taskapplication.data.api.model.MarkAsReadRequest
import com.example.taskapplication.data.api.model.ReactionRequest
import com.example.taskapplication.data.api.model.SendMessageRequest
import com.example.taskapplication.data.api.model.TypingStatusRequest
import com.example.taskapplication.data.database.dao.AttachmentDao
import com.example.taskapplication.data.database.dao.MessageDao
import com.example.taskapplication.data.database.dao.MessageReactionDao
import com.example.taskapplication.data.database.dao.MessageReadStatusDao
import com.example.taskapplication.data.database.entities.MessageEntity
import com.example.taskapplication.data.database.entities.MessageReadStatusEntity
import com.example.taskapplication.data.mapper.toDomainModel
import com.example.taskapplication.data.mapper.toEntity
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.data.websocket.ChatWebSocketClient
import com.example.taskapplication.domain.model.Attachment
import com.example.taskapplication.domain.model.Message
import com.example.taskapplication.domain.model.MessageReaction
import com.example.taskapplication.domain.model.MessageReadStatus
import com.example.taskapplication.domain.repository.MessageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatMessageRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val messageReadStatusDao: MessageReadStatusDao,
    private val messageReactionDao: MessageReactionDao,
    private val attachmentDao: AttachmentDao,
    private val chatApiService: ChatApiService,
    private val webSocketClient: ChatWebSocketClient,
    private val dataStoreManager: DataStoreManager,
    private val connectionChecker: ConnectionChecker
) : MessageRepository, ChatWebSocketClient.MessageHandler {

    private val TAG = "ChatMessageRepository"

    init {
        webSocketClient.setMessageHandler(this)
    }

    override fun getTeamMessages(teamId: String): Flow<List<Message>> {
        return flow {
            // Emit dữ liệu từ cơ sở dữ liệu cục bộ trước
            val localMessages = messageDao.getTeamMessages(teamId)
                .map { entities -> 
                    entities.map { entity ->
                        val attachments = attachmentDao.getAttachmentsByMessageIdSync(entity.id)
                            .map { it.toDomainModel() }
                        entity.toDomainModel(emptyList(), emptyList(), attachments)
                    }
                }
                .first()
            
            emit(localMessages)
            
            // Sau đó tải dữ liệu từ API nếu có kết nối mạng
            if (connectionChecker.isNetworkAvailable()) {
                try {
                    val response = chatApiService.getChatHistory(teamId.toLong())
                    
                    if (response.isSuccessful) {
                        val chatHistory = response.body()
                        if (chatHistory != null) {
                            val serverMessages = chatHistory.data.map { it.toDomainModel() }
                            
                            // Lưu vào cơ sở dữ liệu cục bộ
                            serverMessages.forEach { message ->
                                messageDao.insertMessage(message.toEntity())
                                
                                // Lưu các tệp đính kèm
                                message.attachments.forEach { attachment ->
                                    attachmentDao.insertAttachment(attachment.toEntity().copy(messageId = message.id))
                                }
                            }
                            
                            // Emit lại dữ liệu từ cơ sở dữ liệu cục bộ sau khi cập nhật
                            val updatedMessages = messageDao.getTeamMessages(teamId)
                                .map { entities -> 
                                    entities.map { entity ->
                                        val attachments = attachmentDao.getAttachmentsByMessageIdSync(entity.id)
                                            .map { it.toDomainModel() }
                                        entity.toDomainModel(emptyList(), emptyList(), attachments)
                                    }
                                }
                                .first()
                            
                            emit(updatedMessages)
                        }
                    } else {
                        Log.e(TAG, "Error loading messages from API: ${response.code()} - ${response.message()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading messages from API", e)
                }
            }
        }.catch { e ->
            Log.e(TAG, "Error in getTeamMessages flow", e)
            emit(emptyList())
        }.flowOn(Dispatchers.IO)
    }

    override fun getTeamMessages(teamId: String, limit: Int, beforeId: String?, afterId: String?): Flow<List<Message>> {
        return flow {
            // Emit dữ liệu từ cơ sở dữ liệu cục bộ trước
            val localMessages = messageDao.getTeamMessages(teamId)
                .map { entities -> 
                    var filteredEntities = entities
                    
                    // Lọc theo beforeId nếu có
                    if (beforeId != null) {
                        val beforeMessage = messageDao.getMessageSync(beforeId)
                        if (beforeMessage != null) {
                            filteredEntities = filteredEntities.filter { it.timestamp < beforeMessage.timestamp }
                        }
                    }
                    
                    // Lọc theo afterId nếu có
                    if (afterId != null) {
                        val afterMessage = messageDao.getMessageSync(afterId)
                        if (afterMessage != null) {
                            filteredEntities = filteredEntities.filter { it.timestamp > afterMessage.timestamp }
                        }
                    }
                    
                    // Giới hạn số lượng
                    if (filteredEntities.size > limit) {
                        filteredEntities = filteredEntities.take(limit)
                    }
                    
                    filteredEntities.map { entity ->
                        val attachments = attachmentDao.getAttachmentsByMessageIdSync(entity.id)
                            .map { it.toDomainModel() }
                        entity.toDomainModel(emptyList(), emptyList(), attachments)
                    }
                }
                .first()
            
            emit(localMessages)
            
            // Sau đó tải dữ liệu từ API nếu có kết nối mạng
            if (connectionChecker.isNetworkAvailable()) {
                try {
                    val beforeIdLong = beforeId?.toLongOrNull()
                    val afterIdLong = afterId?.toLongOrNull()
                    
                    val response = chatApiService.getChatHistory(
                        teamId = teamId.toLong(),
                        limit = limit,
                        beforeId = beforeIdLong,
                        afterId = afterIdLong
                    )
                    
                    if (response.isSuccessful) {
                        val chatHistory = response.body()
                        if (chatHistory != null) {
                            val serverMessages = chatHistory.data.map { it.toDomainModel() }
                            
                            // Lưu vào cơ sở dữ liệu cục bộ
                            serverMessages.forEach { message ->
                                messageDao.insertMessage(message.toEntity())
                                
                                // Lưu các tệp đính kèm
                                message.attachments.forEach { attachment ->
                                    attachmentDao.insertAttachment(attachment.toEntity().copy(messageId = message.id))
                                }
                            }
                            
                            // Emit lại dữ liệu từ cơ sở dữ liệu cục bộ sau khi cập nhật
                            val updatedMessages = messageDao.getTeamMessages(teamId)
                                .map { entities -> 
                                    var filteredEntities = entities
                                    
                                    // Lọc theo beforeId nếu có
                                    if (beforeId != null) {
                                        val beforeMessage = messageDao.getMessageSync(beforeId)
                                        if (beforeMessage != null) {
                                            filteredEntities = filteredEntities.filter { it.timestamp < beforeMessage.timestamp }
                                        }
                                    }
                                    
                                    // Lọc theo afterId nếu có
                                    if (afterId != null) {
                                        val afterMessage = messageDao.getMessageSync(afterId)
                                        if (afterMessage != null) {
                                            filteredEntities = filteredEntities.filter { it.timestamp > afterMessage.timestamp }
                                        }
                                    }
                                    
                                    // Giới hạn số lượng
                                    if (filteredEntities.size > limit) {
                                        filteredEntities = filteredEntities.take(limit)
                                    }
                                    
                                    filteredEntities.map { entity ->
                                        val attachments = attachmentDao.getAttachmentsByMessageIdSync(entity.id)
                                            .map { it.toDomainModel() }
                                        entity.toDomainModel(emptyList(), emptyList(), attachments)
                                    }
                                }
                                .first()
                            
                            emit(updatedMessages)
                        }
                    } else {
                        Log.e(TAG, "Error loading messages from API: ${response.code()} - ${response.message()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading messages from API", e)
                }
            }
        }.catch { e ->
            Log.e(TAG, "Error in getTeamMessages flow", e)
            emit(emptyList())
        }.flowOn(Dispatchers.IO)
    }
