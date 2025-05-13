package com.example.taskapplication.data.api

import com.example.taskapplication.data.api.model.AttachmentResponse
import com.example.taskapplication.data.api.model.ChatHistoryResponse
import com.example.taskapplication.data.api.model.DeleteMessageResponse
import com.example.taskapplication.data.api.model.EditMessageRequest
import com.example.taskapplication.data.api.model.MarkAsReadRequest
import com.example.taskapplication.data.api.model.MarkAsReadResponse
import com.example.taskapplication.data.api.model.MessageResponse
import com.example.taskapplication.data.api.model.ReactionRequest
import com.example.taskapplication.data.api.model.ReactionResponse
import com.example.taskapplication.data.api.model.SearchMessagesResponse
import com.example.taskapplication.data.api.model.SendMessageRequest
import com.example.taskapplication.data.api.model.TypingStatusRequest
import com.example.taskapplication.data.api.model.TypingStatusResponse
import com.example.taskapplication.data.api.model.UnreadCountResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApiService {
    @GET("teams/{teamId}/chat")
    suspend fun getChatHistory(
        @Path("teamId") teamId: Long,
        @Query("limit") limit: Int? = null,
        @Query("before_id") beforeId: Long? = null,
        @Query("after_id") afterId: Long? = null
    ): Response<ChatHistoryResponse>

    @POST("teams/{teamId}/chat")
    suspend fun sendMessage(
        @Path("teamId") teamId: Long,
        @Body request: SendMessageRequest
    ): Response<MessageResponse>

    @PUT("teams/{teamId}/chat/{messageId}")
    suspend fun editMessage(
        @Path("teamId") teamId: Long,
        @Path("messageId") messageId: Long,
        @Body request: EditMessageRequest
    ): Response<MessageResponse>

    @DELETE("teams/{teamId}/chat/{messageId}")
    suspend fun deleteMessage(
        @Path("teamId") teamId: Long,
        @Path("messageId") messageId: Long
    ): Response<DeleteMessageResponse>

    @POST("teams/{teamId}/chat/read")
    suspend fun markAsRead(
        @Path("teamId") teamId: Long,
        @Body request: MarkAsReadRequest
    ): Response<MarkAsReadResponse>

    @POST("teams/{teamId}/chat/typing")
    suspend fun updateTypingStatus(
        @Path("teamId") teamId: Long,
        @Body request: TypingStatusRequest
    ): Response<TypingStatusResponse>

    @POST("teams/{teamId}/chat/retry/{clientTempId}")
    suspend fun retryMessage(
        @Path("teamId") teamId: Long,
        @Path("clientTempId") clientTempId: String
    ): Response<MessageResponse>

    @POST("teams/{teamId}/chat/{messageId}/react")
    suspend fun reactToMessage(
        @Path("teamId") teamId: Long,
        @Path("messageId") messageId: Long,
        @Body request: ReactionRequest
    ): Response<ReactionResponse>

    @Multipart
    @POST("teams/{teamId}/chat/attachments")
    suspend fun uploadAttachment(
        @Path("teamId") teamId: Long,
        @Part file: MultipartBody.Part,
        @Part("type") type: RequestBody
    ): Response<AttachmentResponse>

    @GET("teams/{teamId}/chat/unread-count")
    suspend fun getUnreadCount(
        @Path("teamId") teamId: Long
    ): Response<UnreadCountResponse>

    @GET("teams/{teamId}/chat/search")
    suspend fun searchMessages(
        @Path("teamId") teamId: Long,
        @Query("q") query: String,
        @Query("limit") limit: Int? = null,
        @Query("from_date") fromDate: String? = null,
        @Query("to_date") toDate: String? = null,
        @Query("user_id") userId: Long? = null
    ): Response<SearchMessagesResponse>
}
