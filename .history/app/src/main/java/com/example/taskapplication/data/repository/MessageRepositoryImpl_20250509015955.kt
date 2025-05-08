package com.example.taskapplication.data.repository

import android.util.Log
import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.database.dao.MessageDao
import com.example.taskapplication.data.database.dao.MessageReactionDao
import com.example.taskapplication.data.database.dao.MessageReadStatusDao
import com.example.taskapplication.data.database.entities.MessageEntity
import com.example.taskapplication.data.database.entities.MessageReadStatusEntity
import com.example.taskapplication.data.mapper.toDomainModel
import com.example.taskapplication.data.mapper.toEntity
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.domain.model.Message
import com.example.taskapplication.domain.model.MessageReaction
import com.example.taskapplication.domain.model.MessageReadStatus
import com.example.taskapplication.domain.repository.MessageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val messageReadStatusDao: MessageReadStatusDao,
    private val messageReactionDao: MessageReactionDao,
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager,
    private val connectionChecker: ConnectionChecker
) : MessageRepository {

    private val TAG = "MessageRepository"

    override fun getTeamMessages(teamId: String): Flow<List<Message>> {
        return messageDao.getTeamMessages(teamId)
            .map { entities -> entities.map { it.toDomainModel(emptyList(), emptyList()) } }
            .flowOn(Dispatchers.IO)
    }

    override fun getDirectMessages(userId1: String, userId2: String): Flow<List<Message>> {
        return messageDao.getDirectMessages(userId1, userId2)
            .map { entities -> entities.map { it.toDomainModel(emptyList(), emptyList()) } }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getMessageById(id: String): Message? {
        val message = messageDao.getMessage(id) ?: return null
        // Trong thực tế, bạn sẽ cần lấy thêm thông tin về readBy và reactions
        return message.toDomainModel(emptyList(), emptyList())
    }

    override suspend fun sendTeamMessage(teamId: String, content: String): Result<Message> {
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
                isRead = false
            )

            messageDao.insertMessage(messageEntity)

            // Nếu có kết nối mạng, gửi lên server
            if (connectionChecker.isNetworkAvailable()) {
                try {
                    // Triển khai gửi lên server ở đây
                    // Hiện tại chỉ trả về thành công vì chúng ta đã lưu vào local database
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending message to server", e)
                    // Không trả về lỗi vì đã lưu thành công vào local database
                }
            }

            return Result.success(messageEntity.toDomainModel(emptyList(), emptyList()))
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

            return Result.success(messageEntity.toDomainModel(emptyList(), emptyList()))
        } catch (e: Exception) {
            Log.e(TAG, "Error sending direct message", e)
            return Result.failure(e)
        }
    }

    override suspend fun updateMessage(message: Message): Result<Message> {
        try {
            val messageEntity = message.toEntity().copy(
                syncStatus = "pending_update",
                lastModified = System.currentTimeMillis()
            )

            messageDao.updateMessage(messageEntity)

            // Nếu có kết nối mạng, đồng bộ lên server
            if (connectionChecker.isNetworkAvailable()) {
                try {
                    // Triển khai đồng bộ với server ở đây
                    // Hiện tại chỉ trả về thành công vì chúng ta đã lưu vào local database
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating message on server", e)
                    // Không trả về lỗi vì đã lưu thành công vào local database
                }
            }

            return Result.success(messageEntity.toDomainModel(emptyList(), emptyList()))
        } catch (e: Exception) {
            Log.e(TAG, "Error updating message", e)
            return Result.failure(e)
        }
    }

    override suspend fun deleteMessage(messageId: String): Result<Unit> {
        try {
            val message = messageDao.getMessage(messageId)
            if (message != null) {
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
                if (connectionChecker.isNetworkAvailable() && message.serverId != null) {
                    try {
                        // Triển khai xóa trên server ở đây
                        // Hiện tại chỉ trả về thành công vì chúng ta đã xử lý trong local database
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
            if (message != null && !message.isRead) {
                val updatedMessage = message.copy(
                    isRead = true,
                    syncStatus = if (message.syncStatus == "synced") "pending_update" else message.syncStatus,
                    lastModified = System.currentTimeMillis()
                )
                messageDao.updateMessage(updatedMessage)

                // Lưu trạng thái đọc
                val currentUserId = dataStoreManager.getCurrentUserId() ?: return Result.failure(IOException("User not logged in"))
                val readStatus = MessageReadStatusEntity(
                    id = UUID.randomUUID().toString(),
                    messageId = messageId,
                    userId = currentUserId,
                    readAt = System.currentTimeMillis(),
                    serverId = null,
                    syncStatus = "pending_create",
                    lastModified = System.currentTimeMillis()
                )
                messageReadStatusDao.insertReadStatus(readStatus)

                // Nếu có kết nối mạng, đồng bộ lên server
                if (connectionChecker.isNetworkAvailable() && message.serverId != null) {
                    try {
                        // Triển khai đồng bộ với server ở đây
                        // Hiện tại chỉ trả về thành công vì chúng ta đã lưu vào local database
                    } catch (e: Exception) {
                        Log.e(TAG, "Error marking message as read on server", e)
                        // Không trả về lỗi vì đã lưu thành công vào local database
                    }
                }
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking message as read", e)
            return Result.failure(e)
        }
    }

    override suspend fun addReaction(messageId: String, reaction: String): Result<MessageReaction> {
        // Triển khai thêm reaction
        return Result.success(MessageReaction("1", messageId, "1", reaction, System.currentTimeMillis()))
    }

    override suspend fun removeReaction(messageId: String, reactionId: String): Result<Unit> {
        // Triển khai xóa reaction
        return Result.success(Unit)
    }

    override suspend fun getUnreadMessageCount(): Result<Map<String, Int>> {
        // Triển khai lấy số lượng tin nhắn chưa đọc
        return Result.success(emptyMap())
    }

    override suspend fun getOlderTeamMessages(teamId: String, olderThan: Long, limit: Int): Result<List<Message>> {
        // Triển khai lấy tin nhắn cũ hơn
        return Result.success(emptyList())
    }

    override suspend fun syncMessages(): Result<Unit> {
        // Triển khai đồng bộ tin nhắn
        return Result.success(Unit)
    }

    override suspend fun syncTeamMessages(teamId: String): Result<Unit> {
        // Triển khai đồng bộ tin nhắn của team
        return Result.success(Unit)
    }

    override suspend fun saveMessage(message: Message) {
        messageDao.insertMessage(message.toEntity())
    }

    override suspend fun saveReadStatus(readStatus: MessageReadStatus) {
        // Triển khai lưu trạng thái đọc
    }

    override suspend fun markMessageAsDeleted(messageId: Long) {
        // Triển khai đánh dấu tin nhắn đã xóa
    }
}
