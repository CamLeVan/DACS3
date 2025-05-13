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
import kotlinx.coroutines.flow.first
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
                            val file = File(attachment.url)
                            if (file.exists()) {
                                val requestFile = RequestBody.create(MediaType.get(attachment.fileType), file)
                                val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)
                                val typeBody = RequestBody.create(MediaType.get("text/plain"), "attachment")

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

    override suspend fun sendDirectMessage(receiverId: String, content: String): Result<Message> {
        try {
            val currentUserId = dataStoreManager.getCurrentUserId() ?: return Result.failure(IOException("User not logged in"))

            // Tạo message mới
            val messageId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()

            val messageEntity = MessageEntity(
                id = messageId,
                content = content,
                senderId = currentUserId,
                teamId = null,
                receiverId = receiverId,
                timestamp = timestamp,
                serverId = null,
                syncStatus = "pending_create",
                lastModified = timestamp,
                createdAt = timestamp,
                isDeleted = false,
                isRead = false
            )

            messageDao.insertMessage(messageEntity)

            // Nếu có kết nối mạng, gửi lên server
            if (connectionChecker.isNetworkAvailable()) {
                try {
                    // Triển khai gửi lên server ở đây
                    // Hiện tại chỉ trả về thành công vì chúng ta đã lưu vào local database
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending direct message to server", e)
                    // Không trả về lỗi vì đã lưu thành công vào local database
                }
            }

            return Result.success(messageEntity.toDomainModel(emptyList(), emptyList(), emptyList()))
        } catch (e: Exception) {
            Log.e(TAG, "Error sending direct message", e)
            return Result.failure(e)
        }
    }

    override suspend fun retrySendMessage(clientTempId: String): Result<Message> {
        try {
            // Tìm tin nhắn theo clientTempId
            val message = messageDao.getMessageByClientTempId(clientTempId)
                ?: return Result.failure(IOException("Message not found"))

            // Cập nhật trạng thái để gửi lại
            val updatedMessage = message.copy(
                syncStatus = "pending_create",
                lastModified = System.currentTimeMillis()
            )

            messageDao.updateMessage(updatedMessage)

            // Nếu có kết nối mạng, gửi lên server
            if (connectionChecker.isNetworkAvailable() && message.teamId != null) {
                try {
                    val response = chatApiService.retryMessage(message.teamId.toLong(), clientTempId)
                    if (response.isSuccessful) {
                        val messageResponse = response.body()
                        if (messageResponse != null) {
                            val apiMessage = messageResponse.data
                            val domainMessage = apiMessage.toDomainModel()

                            // Cập nhật tin nhắn trong cơ sở dữ liệu cục bộ
                            val serverUpdatedMessage = updatedMessage.copy(
                                serverId = apiMessage.id.toString(),
                                syncStatus = "synced"
                            )

                            messageDao.updateMessage(serverUpdatedMessage)

                            // Cập nhật tệp đính kèm
                            apiMessage.attachments.forEach { apiAttachment ->
                                val attachment = apiAttachment.toDomainModel()
                                attachmentDao.insertAttachment(attachment.toEntity().copy(messageId = message.id))
                            }

                            return Result.success(domainMessage)
                        }
                    } else {
                        Log.e(TAG, "Error retrying message: ${response.code()} - ${response.message()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error retrying message", e)
                    // Không trả về lỗi vì đã lưu vào cơ sở dữ liệu cục bộ
                }
            }

            // Lấy danh sách tệp đính kèm
            val attachments = attachmentDao.getAttachmentsByMessageIdSync(updatedMessage.id)
                .map { it.toDomainModel() }

            return Result.success(updatedMessage.toDomainModel(emptyList(), emptyList(), attachments))
        } catch (e: Exception) {
            Log.e(TAG, "Error retrying message", e)
            return Result.failure(e)
        }
    }

    override suspend fun updateMessage(message: Message): Result<Message> {
        return editMessage(message.id, message.content)
    }

    override suspend fun getOlderTeamMessages(teamId: String, olderThan: Long, limit: Int): Result<List<Message>> {
        try {
            // Lấy tin nhắn từ cơ sở dữ liệu cục bộ
            val localMessages = messageDao.getTeamMessages(teamId)
                .first()
                .filter { it.timestamp < olderThan }
                .sortedByDescending { it.timestamp }
                .take(limit)
                .map { entity ->
                    val attachments = attachmentDao.getAttachmentsByMessageIdSync(entity.id)
                        .map { it.toDomainModel() }
                    entity.toDomainModel(emptyList(), emptyList(), attachments)
                }

            // Nếu có kết nối mạng, thử lấy từ server
            if (connectionChecker.isNetworkAvailable()) {
                try {
                    // Tìm tin nhắn cũ nhất trong danh sách hiện tại
                    val oldestMessage = localMessages.minByOrNull { it.timestamp }

                    val response = chatApiService.getChatHistory(
                        teamId = teamId.toLong(),
                        limit = limit,
                        beforeId = oldestMessage?.serverId?.toLongOrNull(),
                        afterId = null
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

                            // Lấy lại danh sách tin nhắn từ cơ sở dữ liệu cục bộ
                            val updatedMessages = messageDao.getTeamMessages(teamId)
                                .first()
                                .filter { it.timestamp < olderThan }
                                .sortedByDescending { it.timestamp }
                                .take(limit)
                                .map { entity ->
                                    val attachments = attachmentDao.getAttachmentsByMessageIdSync(entity.id)
                                        .map { it.toDomainModel() }
                                    entity.toDomainModel(emptyList(), emptyList(), attachments)
                                }

                            return Result.success(updatedMessages)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading older messages from API", e)
                    // Không trả về lỗi vì đã có dữ liệu từ cơ sở dữ liệu cục bộ
                }
            }

            return Result.success(localMessages)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting older team messages", e)
            return Result.failure(e)
        }
    }

    override suspend fun editMessage(messageId: String, newContent: String): Result<Message> {
        try {
            val message = messageDao.getMessage(messageId)
                ?: return Result.failure(IOException("Message not found"))

            // Kiểm tra quyền chỉnh sửa (chỉ người gửi mới có quyền chỉnh sửa)
            val currentUserId = dataStoreManager.getCurrentUserId() ?: return Result.failure(IOException("User not logged in"))
            if (message.senderId != currentUserId) {
                return Result.failure(IOException("Bạn không có quyền chỉnh sửa tin nhắn này"))
            }

            // Cập nhật nội dung tin nhắn
            val updatedMessage = message.copy(
                content = newContent,
                syncStatus = "pending_update",
                lastModified = System.currentTimeMillis()
            )

            messageDao.updateMessage(updatedMessage)

            // Nếu có kết nối mạng, đồng bộ lên server
            if (connectionChecker.isNetworkAvailable() && message.teamId != null && message.serverId != null) {
                try {
                    val request = EditMessageRequest(newContent)
                    val response = chatApiService.editMessage(
                        message.teamId.toLong(),
                        message.serverId.toLong(),
                        request
                    )

                    if (response.isSuccessful) {
                        val messageResponse = response.body()
                        if (messageResponse != null) {
                            val apiMessage = messageResponse.data
                            val domainMessage = apiMessage.toDomainModel()

                            // Cập nhật tin nhắn trong cơ sở dữ liệu cục bộ
                            val serverUpdatedMessage = updatedMessage.copy(
                                syncStatus = "synced"
                            )

                            messageDao.updateMessage(serverUpdatedMessage)

                            return Result.success(domainMessage)
                        }
                    } else {
                        Log.e(TAG, "Error editing message: ${response.code()} - ${response.message()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error editing message on server", e)
                    // Không trả về lỗi vì đã lưu thành công vào local database
                }
            }

            // Lấy danh sách tệp đính kèm
            val attachments = attachmentDao.getAttachmentsByMessageIdSync(updatedMessage.id)
                .map { it.toDomainModel() }

            return Result.success(updatedMessage.toDomainModel(emptyList(), emptyList(), attachments))
        } catch (e: Exception) {
            Log.e(TAG, "Error editing message", e)
            return Result.failure(e)
        }
    }

    override suspend fun deleteMessage(messageId: String): Result<Unit> {
        try {
            val message = messageDao.getMessage(messageId)
            if (message != null) {
                // Kiểm tra quyền xóa (chỉ người gửi mới có quyền xóa)
                val currentUserId = dataStoreManager.getCurrentUserId() ?: return Result.failure(IOException("User not logged in"))
                if (message.senderId != currentUserId) {
                    return Result.failure(IOException("Bạn không có quyền xóa tin nhắn này"))
                }

                // Nếu message đã được đồng bộ với server, đánh dấu để xóa sau
                if (message.serverId != null) {
                    val updatedMessage = message.copy(
                        syncStatus = "pending_delete",
                        lastModified = System.currentTimeMillis(),
                        isDeleted = true
                    )
                    messageDao.updateMessage(updatedMessage)
                } else {
                    // Nếu message chưa được đồng bộ với server, xóa luôn
                    messageDao.deleteMessage(message)
                }

                // Nếu có kết nối mạng, đồng bộ lên server
                if (connectionChecker.isNetworkAvailable() && message.teamId != null && message.serverId != null) {
                    try {
                        val response = chatApiService.deleteMessage(
                            message.teamId.toLong(),
                            message.serverId.toLong()
                        )

                        if (!response.isSuccessful) {
                            Log.e(TAG, "Error deleting message: ${response.code()} - ${response.message()}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting message on server", e)
                        // Không trả về lỗi vì đã xử lý thành công trong local database
                    }
                }
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting message", e)
            return Result.failure(e)
        }
    }

    override suspend fun markMessageAsRead(messageId: String): Result<Unit> {
        try {
            val message = messageDao.getMessage(messageId)
                ?: return Result.failure(IOException("Message not found"))

            val currentUserId = dataStoreManager.getCurrentUserId() ?: return Result.failure(IOException("User not logged in"))

            // Kiểm tra xem tin nhắn đã được đánh dấu là đã đọc chưa
            val existingReadStatus = messageReadStatusDao.getReadStatus(messageId, currentUserId)
            if (existingReadStatus != null) {
                return Result.success(Unit) // Đã đánh dấu là đã đọc rồi
            }

            // Tạo trạng thái đã đọc mới
            val readStatusId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()

            val readStatusEntity = MessageReadStatusEntity(
                id = readStatusId,
                messageId = messageId,
                userId = currentUserId,
                readAt = timestamp,
                serverId = null,
                syncStatus = "pending_create",
                lastModified = timestamp
            )

            messageReadStatusDao.insertReadStatus(readStatusEntity)

            // Cập nhật trạng thái đã đọc của tin nhắn
            val updatedMessage = message.copy(
                isRead = true,
                lastModified = timestamp
            )

            messageDao.updateMessage(updatedMessage)

            // Nếu có kết nối mạng, đồng bộ lên server
            if (connectionChecker.isNetworkAvailable() && message.teamId != null && message.serverId != null) {
                try {
                    val request = MarkAsReadRequest(message.serverId.toLong())
                    val response = chatApiService.markAsRead(
                        message.teamId.toLong(),
                        request
                    )

                    if (!response.isSuccessful) {
                        Log.e(TAG, "Error marking message as read: ${response.code()} - ${response.message()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error marking message as read on server", e)
                    // Không trả về lỗi vì đã xử lý thành công trong local database
                }
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking message as read", e)
            return Result.failure(e)
        }
    }

    override suspend fun sendTypingStatus(teamId: String, isTyping: Boolean): Result<Unit> {
        try {
            // Nếu có kết nối mạng, gửi trạng thái đang nhập lên server
            if (connectionChecker.isNetworkAvailable()) {
                try {
                    val request = TypingStatusRequest(isTyping)
                    val response = chatApiService.updateTypingStatus(
                        teamId.toLong(),
                        request
                    )

                    if (!response.isSuccessful) {
                        Log.e(TAG, "Error sending typing status: ${response.code()} - ${response.message()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending typing status", e)
                    return Result.failure(e)
                }
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending typing status", e)
            return Result.failure(e)
        }
    }

    override suspend fun addReaction(messageId: String, reaction: String): Result<MessageReaction> {
        try {
            val message = messageDao.getMessage(messageId)
                ?: return Result.failure(IOException("Message not found"))

            val currentUserId = dataStoreManager.getCurrentUserId() ?: return Result.failure(IOException("User not logged in"))

            // Kiểm tra xem đã có reaction này chưa
            val existingReaction = messageReactionDao.getReaction(messageId, currentUserId, reaction)
            if (existingReaction != null) {
                return Result.success(existingReaction.toDomainModel())
            }

            // Tạo reaction mới
            val reactionId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()

            val reactionEntity = MessageReactionEntity(
                id = reactionId,
                messageId = messageId,
                userId = currentUserId,
                reaction = reaction,
                serverId = null,
                timestamp = timestamp
            )

            messageReactionDao.insertReaction(reactionEntity)

            // Nếu có kết nối mạng, đồng bộ lên server
            if (connectionChecker.isNetworkAvailable() && message.teamId != null && message.serverId != null) {
                try {
                    val request = ReactionRequest(reaction)
                    val response = chatApiService.reactToMessage(
                        message.teamId.toLong(),
                        message.serverId.toLong(),
                        request
                    )

                    if (!response.isSuccessful) {
                        Log.e(TAG, "Error adding reaction: ${response.code()} - ${response.message()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error adding reaction on server", e)
                    // Không trả về lỗi vì đã xử lý thành công trong local database
                }
            }

            return Result.success(reactionEntity.toDomainModel())
        } catch (e: Exception) {
            Log.e(TAG, "Error adding reaction", e)
            return Result.failure(e)
        }
    }

    override suspend fun removeReaction(messageId: String, reaction: String): Result<Unit> {
        try {
            val message = messageDao.getMessage(messageId)
                ?: return Result.failure(IOException("Message not found"))

            val currentUserId = dataStoreManager.getCurrentUserId() ?: return Result.failure(IOException("User not logged in"))

            // Xóa reaction
            messageReactionDao.deleteReaction(messageId, currentUserId, reaction)

            // Nếu có kết nối mạng, đồng bộ lên server
            if (connectionChecker.isNetworkAvailable() && message.teamId != null && message.serverId != null) {
                try {
                    val request = ReactionRequest("") // Gửi chuỗi rỗng để xóa reaction
                    val response = chatApiService.reactToMessage(
                        message.teamId.toLong(),
                        message.serverId.toLong(),
                        request
                    )

                    if (!response.isSuccessful) {
                        Log.e(TAG, "Error removing reaction: ${response.code()} - ${response.message()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing reaction on server", e)
                    // Không trả về lỗi vì đã xử lý thành công trong local database
                }
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing reaction", e)
            return Result.failure(e)
        }
    }

    // Triển khai MessageHandler
    override fun onConnectionOpened() {
        Log.d(TAG, "WebSocket connection opened")
    }

    override fun onNewMessage(message: ApiMessage) {
        try {
            val domainMessage = message.toDomainModel()
            val messageEntity = domainMessage.toEntity()

            // Lưu tin nhắn vào cơ sở dữ liệu cục bộ
            viewModelScope.launch(Dispatchers.IO) {
                messageDao.insertMessage(messageEntity)

                // Lưu các tệp đính kèm
                message.attachments.forEach { apiAttachment ->
                    val attachment = apiAttachment.toDomainModel()
                    attachmentDao.insertAttachment(attachment.toEntity().copy(messageId = messageEntity.id))
                }

                Log.d(TAG, "New message received and saved: ${message.id}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling new message", e)
        }
    }

    override fun onMessageUpdated(message: ApiMessageUpdate) {
        try {
            // Tìm tin nhắn trong cơ sở dữ liệu cục bộ
            val existingMessage = messageDao.getMessageByServerId(message.id.toString())
            if (existingMessage != null) {
                // Cập nhật nội dung tin nhắn
                val updatedMessage = existingMessage.copy(
                    content = message.message,
                    lastModified = System.currentTimeMillis(),
                    syncStatus = "synced"
                )

                messageDao.updateMessage(updatedMessage)
                Log.d(TAG, "Message updated: ${message.id}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message update", e)
        }
    }

    override fun onMessageDeleted(message: ApiMessageDelete) {
        try {
            // Tìm tin nhắn trong cơ sở dữ liệu cục bộ
            val existingMessage = messageDao.getMessageByServerId(message.id.toString())
            if (existingMessage != null) {
                // Đánh dấu tin nhắn đã bị xóa
                val updatedMessage = existingMessage.copy(
                    isDeleted = true,
                    lastModified = System.currentTimeMillis(),
                    syncStatus = "synced"
                )

                messageDao.updateMessage(updatedMessage)
                Log.d(TAG, "Message deleted: ${message.id}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message deletion", e)
        }
    }

    override fun onMessageRead(readStatus: ApiReadStatus) {
        try {
            // Tìm tin nhắn trong cơ sở dữ liệu cục bộ
            val existingMessage = messageDao.getMessageByServerId(readStatus.messageId.toString())
            if (existingMessage != null) {
                // Tạo trạng thái đã đọc mới
                val readStatusEntity = MessageReadStatusEntity(
                    id = UUID.randomUUID().toString(),
                    messageId = existingMessage.id,
                    userId = readStatus.userId.toString(),
                    readAt = System.currentTimeMillis(),
                    serverId = null,
                    syncStatus = "synced",
                    lastModified = System.currentTimeMillis()
                )

                messageReadStatusDao.insertReadStatus(readStatusEntity)

                // Cập nhật trạng thái đã đọc của tin nhắn
                val updatedMessage = existingMessage.copy(
                    isRead = true,
                    lastModified = System.currentTimeMillis()
                )

                messageDao.updateMessage(updatedMessage)
                Log.d(TAG, "Message marked as read: ${readStatus.messageId}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message read status", e)
        }
    }

    override fun onTypingStatusChanged(typingStatus: ApiTypingStatus) {
        // Xử lý trạng thái đang nhập
        // Thông thường, trạng thái đang nhập sẽ được xử lý trực tiếp trong ViewModel
        Log.d(TAG, "Typing status changed: user ${typingStatus.userId} is ${if (typingStatus.isTyping) "typing" else "not typing"}")
    }

    override fun onReactionAdded(reaction: ApiReaction) {
        try {
            // Tìm tin nhắn trong cơ sở dữ liệu cục bộ
            val existingMessage = messageDao.getMessageByServerId(reaction.messageId.toString())
            if (existingMessage != null) {
                // Kiểm tra xem đã có reaction này chưa
                val existingReaction = messageReactionDao.getReaction(existingMessage.id, reaction.userId.toString(), reaction.reaction)
                if (existingReaction == null) {
                    // Tạo reaction mới
                    val reactionEntity = MessageReactionEntity(
                        id = UUID.randomUUID().toString(),
                        messageId = existingMessage.id,
                        userId = reaction.userId.toString(),
                        reaction = reaction.reaction,
                        serverId = null,
                        timestamp = System.currentTimeMillis()
                    )

                    messageReactionDao.insertReaction(reactionEntity)
                    Log.d(TAG, "Reaction added: ${reaction.reaction} to message ${reaction.messageId}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling reaction", e)
        }
    }

    override fun onConnectionError(throwable: Throwable) {
        Log.e(TAG, "WebSocket connection error", throwable)
    }

    override fun onConnectionClosed() {
        Log.d(TAG, "WebSocket connection closed")
    }

    // Các phương thức còn thiếu
    override suspend fun getUnreadMessageCount(): Result<Map<String, Int>> {
        return try {
            val result = mutableMapOf<String, Int>()

            // Lấy từ cơ sở dữ liệu cục bộ
            val teams = messageDao.getAllTeams()
            teams.forEach { teamId ->
                val count = messageDao.getUnreadMessageCountByTeam(teamId)
                result[teamId] = count
            }

            // Nếu có kết nối mạng, lấy từ server
            if (connectionChecker.isNetworkAvailable()) {
                try {
                    val response = chatApiService.getUnreadCounts()
                    if (response.isSuccessful) {
                        val unreadCounts = response.body()
                        if (unreadCounts != null) {
                            unreadCounts.data.forEach { (teamId, count) ->
                                result[teamId.toString()] = count
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting unread counts from server", e)
                }
            }

            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting unread message count", e)
            Result.failure(e)
        }
    }

    override suspend fun getTeamUnreadMessageCount(teamId: String): Result<Int> {
        return try {
            // Lấy từ cơ sở dữ liệu cục bộ
            val count = messageDao.getUnreadMessageCountByTeam(teamId)

            // Nếu có kết nối mạng, lấy từ server
            if (connectionChecker.isNetworkAvailable()) {
                try {
                    val response = chatApiService.getTeamUnreadCount(teamId.toLong())
                    if (response.isSuccessful) {
                        val unreadCount = response.body()
                        if (unreadCount != null) {
                            return Result.success(unreadCount.data)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting team unread count from server", e)
                }
            }

            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting team unread message count", e)
            Result.failure(e)
        }
    }

    override suspend fun syncMessages(): Result<Unit> {
        return try {
            // Lấy danh sách tin nhắn chưa đồng bộ
            val pendingMessages = messageDao.getPendingMessages()

            if (pendingMessages.isNotEmpty() && connectionChecker.isNetworkAvailable()) {
                pendingMessages.forEach { message ->
                    when (message.syncStatus) {
                        "pending_create" -> {
                            if (message.teamId != null) {
                                // Gửi tin nhắn lên server
                                val request = SendMessageRequest(
                                    message = message.content,
                                    attachments = null,
                                    clientTempId = message.clientTempId
                                )

                                try {
                                    val response = chatApiService.sendMessage(message.teamId.toLong(), request)
                                    if (response.isSuccessful) {
                                        val messageResponse = response.body()
                                        if (messageResponse != null) {
                                            val apiMessage = messageResponse.data

                                            // Cập nhật tin nhắn trong cơ sở dữ liệu cục bộ
                                            val updatedMessage = message.copy(
                                                serverId = apiMessage.id.toString(),
                                                syncStatus = "synced"
                                            )

                                            messageDao.updateMessage(updatedMessage)
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error syncing message", e)
                                }
                            }
                        }
                        "pending_update" -> {
                            if (message.teamId != null && message.serverId != null) {
                                // Cập nhật tin nhắn trên server
                                val request = EditMessageRequest(message.content)

                                try {
                                    val response = chatApiService.editMessage(
                                        message.teamId.toLong(),
                                        message.serverId.toLong(),
                                        request
                                    )

                                    if (response.isSuccessful) {
                                        // Cập nhật tin nhắn trong cơ sở dữ liệu cục bộ
                                        val updatedMessage = message.copy(
                                            syncStatus = "synced"
                                        )

                                        messageDao.updateMessage(updatedMessage)
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error syncing message update", e)
                                }
                            }
                        }
                        "pending_delete" -> {
                            if (message.teamId != null && message.serverId != null) {
                                // Xóa tin nhắn trên server
                                try {
                                    val response = chatApiService.deleteMessage(
                                        message.teamId.toLong(),
                                        message.serverId.toLong()
                                    )

                                    if (response.isSuccessful) {
                                        // Xóa tin nhắn khỏi cơ sở dữ liệu cục bộ
                                        messageDao.deleteMessage(message)
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error syncing message deletion", e)
                                }
                            }
                        }
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing messages", e)
            Result.failure(e)
        }
    }

    override suspend fun syncTeamMessages(teamId: String): Result<Unit> {
        return try {
            // Lấy danh sách tin nhắn chưa đồng bộ của nhóm
            val pendingMessages = messageDao.getPendingMessagesByTeam(teamId)

            if (pendingMessages.isNotEmpty() && connectionChecker.isNetworkAvailable()) {
                pendingMessages.forEach { message ->
                    when (message.syncStatus) {
                        "pending_create" -> {
                            // Gửi tin nhắn lên server
                            val request = SendMessageRequest(
                                message = message.content,
                                attachments = null,
                                clientTempId = message.clientTempId
                            )

                            try {
                                val response = chatApiService.sendMessage(teamId.toLong(), request)
                                if (response.isSuccessful) {
                                    val messageResponse = response.body()
                                    if (messageResponse != null) {
                                        val apiMessage = messageResponse.data

                                        // Cập nhật tin nhắn trong cơ sở dữ liệu cục bộ
                                        val updatedMessage = message.copy(
                                            serverId = apiMessage.id.toString(),
                                            syncStatus = "synced"
                                        )

                                        messageDao.updateMessage(updatedMessage)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error syncing team message", e)
                            }
                        }
                        "pending_update" -> {
                            if (message.serverId != null) {
                                // Cập nhật tin nhắn trên server
                                val request = EditMessageRequest(message.content)

                                try {
                                    val response = chatApiService.editMessage(
                                        teamId.toLong(),
                                        message.serverId.toLong(),
                                        request
                                    )

                                    if (response.isSuccessful) {
                                        // Cập nhật tin nhắn trong cơ sở dữ liệu cục bộ
                                        val updatedMessage = message.copy(
                                            syncStatus = "synced"
                                        )

                                        messageDao.updateMessage(updatedMessage)
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error syncing team message update", e)
                                }
                            }
                        }
                        "pending_delete" -> {
                            if (message.serverId != null) {
                                // Xóa tin nhắn trên server
                                try {
                                    val response = chatApiService.deleteMessage(
                                        teamId.toLong(),
                                        message.serverId.toLong()
                                    )

                                    if (response.isSuccessful) {
                                        // Xóa tin nhắn khỏi cơ sở dữ liệu cục bộ
                                        messageDao.deleteMessage(message)
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error syncing team message deletion", e)
                                }
                            }
                        }
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

        // Lưu các tệp đính kèm
        message.attachments.forEach { attachment ->
            attachmentDao.insertAttachment(attachment.toEntity().copy(messageId = message.id))
        }
    }

    override suspend fun saveReadStatus(readStatus: MessageReadStatus) {
        messageReadStatusDao.insertReadStatus(readStatus.toEntity())

        // Cập nhật trạng thái đã đọc của tin nhắn
        val message = messageDao.getMessage(readStatus.messageId)
        if (message != null) {
            val updatedMessage = message.copy(
                isRead = true,
                lastModified = System.currentTimeMillis()
            )

            messageDao.updateMessage(updatedMessage)
        }
    }

    override suspend fun markMessageAsDeleted(messageId: String) {
        val message = messageDao.getMessage(messageId)
        if (message != null) {
            val updatedMessage = message.copy(
                isDeleted = true,
                lastModified = System.currentTimeMillis(),
                syncStatus = "synced"
            )

            messageDao.updateMessage(updatedMessage)
        }
    }
}