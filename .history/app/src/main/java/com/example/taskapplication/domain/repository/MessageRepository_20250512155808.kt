package com.example.taskapplication.domain.repository

import com.example.taskapplication.domain.model.Message
import com.example.taskapplication.domain.model.MessageReaction
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    /**
     * Lấy danh sách tin nhắn của một nhóm
     */
    fun getTeamMessages(teamId: String): Flow<List<Message>>

    /**
     * Lấy danh sách tin nhắn của một nhóm với phân trang
     */
    fun getTeamMessages(teamId: String, limit: Int, beforeId: String? = null, afterId: String? = null): Flow<List<Message>>

    /**
     * Lấy danh sách tin nhắn trực tiếp giữa hai người dùng
     */
    fun getDirectMessages(userId1: String, userId2: String): Flow<List<Message>>

    /**
     * Lấy thông tin của một tin nhắn theo ID
     */
    suspend fun getMessageById(id: String): Message?

    /**
     * Gửi tin nhắn đến một nhóm
     */
    suspend fun sendTeamMessage(teamId: String, content: String): Result<Message>

    /**
     * Gửi tin nhắn đến một nhóm với client_temp_id và attachments
     */
    suspend fun sendTeamMessage(
        teamId: String,
        content: String,
        clientTempId: String,
        attachments: List<com.example.taskapplication.domain.model.Attachment>? = null
    ): Result<Message>

    /**
     * Gửi tin nhắn trực tiếp đến một người dùng
     */
    suspend fun sendDirectMessage(receiverId: String, content: String): Result<Message>

    /**
     * Gửi lại tin nhắn khi gặp lỗi
     */
    suspend fun retrySendMessage(clientTempId: String): Result<Message>

    /**
     * Cập nhật nội dung tin nhắn
     */
    suspend fun updateMessage(message: Message): Result<Message>

    /**
     * Cập nhật nội dung tin nhắn theo ID
     */
    suspend fun editMessage(messageId: String, newContent: String): Result<Message>

    /**
     * Xóa tin nhắn
     */
    suspend fun deleteMessage(messageId: String): Result<Unit>

    /**
     * Đánh dấu tin nhắn đã đọc
     */
    suspend fun markMessageAsRead(messageId: String): Result<Unit>

    /**
     * Thêm phản ứng vào tin nhắn
     */
    suspend fun addReaction(messageId: String, reaction: String): Result<MessageReaction>

    /**
     * Xóa phản ứng khỏi tin nhắn
     */
    suspend fun removeReaction(messageId: String, reactionId: String): Result<Unit>

    /**
     * Lấy số lượng tin nhắn chưa đọc
     */
    suspend fun getUnreadMessageCount(): Result<Map<String, Int>>

    /**
     * Lấy số lượng tin nhắn chưa đọc trong một nhóm
     */
    suspend fun getTeamUnreadMessageCount(teamId: String): Result<Int>

    /**
     * Lấy tin nhắn cũ hơn một thời điểm
     */
    suspend fun getOlderTeamMessages(teamId: String, olderThan: Long, limit: Int): Result<List<Message>>

    /**
     * Cập nhật trạng thái đang nhập
     */
    suspend fun sendTypingStatus(teamId: String, isTyping: Boolean): Result<Unit>

    /**
     * Đồng bộ hóa tất cả tin nhắn
     */
    suspend fun syncMessages(): Result<Unit>

    /**
     * Đồng bộ hóa tin nhắn của một nhóm
     */
    suspend fun syncTeamMessages(teamId: String): Result<Unit>

    // WebSocket support
    /**
     * Lưu tin nhắn vào cơ sở dữ liệu
     */
    suspend fun saveMessage(message: Message)

    /**
     * Lưu trạng thái đọc vào cơ sở dữ liệu
     */
    suspend fun saveReadStatus(readStatus: com.example.taskapplication.domain.model.MessageReadStatus)

    /**
     * Đánh dấu tin nhắn đã bị xóa
     */
    suspend fun markMessageAsDeleted(messageId: String)
}