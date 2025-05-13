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

    override fun getDirectMessages(userId1: String, userId2: String): Flow<List<Message>> {
        return messageDao.getDirectMessages(userId1, userId2)
            .map { entities ->
                entities.map { entity ->
                    val attachments = attachmentDao.getAttachmentsByMessageIdSync(entity.id)
                        .map { it.toDomainModel() }
                    entity.toDomainModel(emptyList(), emptyList(), attachments)
                }
            }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getMessageById(id: String): Message? {
        val message = messageDao.getMessage(id) ?: return null
        val attachments = attachmentDao.getAttachmentsByMessageIdSync(message.id)
            .map { it.toDomainModel() }
        return message.toDomainModel(emptyList(), emptyList(), attachments)
    }

    override suspend fun sendTeamMessage(teamId: String, content: String): Result<Message> {
        return sendTeamMessage(teamId, content, UUID.randomUUID().toString(), null)
    }

    override suspend fun sendTeamMessage(
        teamId: String,
        content: String,
        clientTempId: String,
        attachments: List<Attachment>?
    ): Result<Message> {
        try {
            val currentUserId = dataStoreManager.getCurrentUserId() ?: return Result.failure(IOException("User not logged in"))

            // Tạo message mới
            val messageId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()

            val messageEntity = MessageEntity(
                id = messageId,
                content = content,
                senderId = currentUserId,
                teamId = teamId,
                receiverId = null,
                timestamp = timestamp,
                serverId = null,
                syncStatus = "pending_create",
                lastModified = timestamp,
                createdAt = timestamp,
                isDeleted = false,
                isRead = false,
                clientTempId = clientTempId
            )

            messageDao.insertMessage(messageEntity)

            // Lưu các tệp đính kèm nếu có
            val attachmentIds = mutableListOf<Long>()
            if (attachments != null && attachments.isNotEmpty()) {
                for (attachment in attachments) {
                    val attachmentEntity = attachment.toEntity().copy(
                        messageId = messageId,
                        syncStatus = "pending_create"
                    )
                    attachmentDao.insertAttachment(attachmentEntity)
                }
            }

            // Nếu có kết nối mạng, gửi lên server
            if (connectionChecker.isNetworkAvailable()) {
                try {
                    // Tải lên tệp đính kèm trước nếu có
                    if (attachments != null && attachments.isNotEmpty()) {
                        for (attachment in attachments) {
                            val file = File(attachment.filePath)
                            if (file.exists()) {
                                val requestFile = RequestBody.create(MediaType.parse(attachment.mimeType), file)
                                val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)
                                val typeBody = RequestBody.create(MediaType.parse("text/plain"), attachment.type)

                                val response = chatApiService.uploadAttachment(teamId.toLong(), filePart, typeBody)
                                if (response.isSuccessful) {
                                    val uploadedAttachment = response.body()?.data
                                    if (uploadedAttachment != null) {
                                        attachmentIds.add(uploadedAttachment.id)
                                    }
                                }
                            }
                        }
                    }

                    // Gửi tin nhắn với ID tệp đính kèm
                    val request = SendMessageRequest(
                        message = content,
                        attachments = if (attachmentIds.isNotEmpty()) attachmentIds else null,
                        clientTempId = clientTempId
                    )

                    val response = chatApiService.sendMessage(teamId.toLong(), request)
                    if (response.isSuccessful) {
                        val messageResponse = response.body()
                        if (messageResponse != null) {
                            val apiMessage = messageResponse.data
                            val domainMessage = apiMessage.toDomainModel()

                            // Cập nhật tin nhắn trong cơ sở dữ liệu cục bộ
                            val updatedMessage = messageEntity.copy(
                                serverId = apiMessage.id.toString(),
                                syncStatus = "synced"
                            )

                            messageDao.updateMessage(updatedMessage)

                            // Cập nhật tệp đính kèm
                            apiMessage.attachments.forEach { apiAttachment ->
                                val attachment = apiAttachment.toDomainModel()
                                attachmentDao.insertAttachment(attachment.toEntity().copy(messageId = messageId))
                            }

                            return Result.success(domainMessage)
                        }
                    } else {
                        Log.e(TAG, "Error sending message to server: ${response.code()} - ${response.message()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending message to server", e)
                    // Không trả về lỗi vì đã lưu vào cơ sở dữ liệu cục bộ
                }
            }

            // Lấy danh sách tệp đính kèm để trả về
            val savedAttachments = if (attachments != null && attachments.isNotEmpty()) {
                attachments
            } else {
                emptyList()
            }

            return Result.success(messageEntity.toDomainModel(emptyList(), emptyList(), savedAttachments))
        } catch (e: Exception) {
            Log.e(TAG, "Error sending team message", e)
            return Result.failure(e)
        }
    }