package com.example.taskapplication.data.api

import com.example.taskapplication.data.api.request.*
import com.example.taskapplication.data.api.response.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // Auth
    @POST("auth/register")
    suspend fun register(@Body registerRequest: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<AuthResponse>

    @POST("auth/google")
    suspend fun loginWithGoogle(@Body googleAuthRequest: GoogleAuthRequest): Response<AuthResponse>

    @POST("auth/logout")
    suspend fun logout(@Query("device_id") deviceId: String): Response<Unit>

    @POST("auth/biometric/register")
    suspend fun registerBiometric(@Body request: BiometricRegisterRequest): Response<Unit>

    @POST("auth/biometric/verify")
    suspend fun verifyBiometric(@Body request: BiometricVerifyRequest): Response<AuthResponse>

    // Personal Tasks
    @GET("personal-tasks")
    suspend fun getPersonalTasks(): Response<List<PersonalTaskResponse>>

    @GET("personal-tasks/{id}")
    suspend fun getPersonalTask(@Path("id") id: String): Response<PersonalTaskResponse>

    @POST("personal-tasks")
    suspend fun createPersonalTask(@Body request: PersonalTaskRequest): Response<PersonalTaskResponse>

    @PUT("personal-tasks/{id}")
    suspend fun updatePersonalTask(
        @Path("id") id: String,
        @Body request: PersonalTaskRequest
    ): Response<PersonalTaskResponse>

    @DELETE("personal-tasks/{id}")
    suspend fun deletePersonalTask(@Path("id") id: String): Response<Unit>

    // Team Tasks
    @GET("team-tasks")
    suspend fun getTeamTasks(): Response<List<TeamTaskResponse>>

    @GET("team-tasks/{id}")
    suspend fun getTeamTask(@Path("id") id: String): Response<TeamTaskResponse>

    @POST("team-tasks")
    suspend fun createTeamTask(@Body request: TeamTaskRequest): Response<TeamTaskResponse>

    @PUT("team-tasks/{id}")
    suspend fun updateTeamTask(
        @Path("id") id: String,
        @Body request: TeamTaskRequest
    ): Response<TeamTaskResponse>

    @DELETE("team-tasks/{id}")
    suspend fun deleteTeamTask(@Path("id") id: String): Response<Unit>

    // Messages
    @GET("messages")
    suspend fun getMessages(): Response<List<MessageResponse>>

    @GET("messages/{id}")
    suspend fun getMessage(@Path("id") id: String): Response<MessageResponse>

    @POST("messages")
    suspend fun createMessage(@Body request: MessageRequest): Response<MessageResponse>

    @PUT("messages/{id}")
    suspend fun updateMessage(
        @Path("id") id: String,
        @Body request: MessageRequest
    ): Response<MessageResponse>

    @DELETE("messages/{id}")
    suspend fun deleteMessage(@Path("id") id: String): Response<Unit>

    @POST("messages/{id}/read")
    suspend fun markMessageAsRead(@Path("id") id: String, @Query("userId") userId: String): Response<Unit>

    @POST("messages/{id}/reactions")
    suspend fun addReaction(@Path("id") id: String, @Query("userId") userId: String, @Query("reaction") reaction: String): Response<Unit>

    @DELETE("messages/{id}/reactions")
    suspend fun removeReaction(@Path("id") id: String, @Query("userId") userId: String, @Query("reaction") reaction: String): Response<Unit>

    // Teams
    @GET("teams")
    suspend fun getTeams(): Response<List<TeamResponse>>

    @GET("teams/{id}")
    suspend fun getTeam(@Path("id") id: String): Response<TeamResponse>

    @POST("teams")
    suspend fun createTeam(@Body request: TeamRequest): Response<TeamResponse>

    @PUT("teams/{id}")
    suspend fun updateTeam(
        @Path("id") id: String,
        @Body request: TeamRequest
    ): Response<TeamResponse>

    @DELETE("teams/{id}")
    suspend fun deleteTeam(@Path("id") id: String): Response<Unit>

    // Team Members
    @GET("teams/{teamId}/members")
    suspend fun getTeamMembers(@Path("teamId") teamId: String): Response<List<TeamMemberResponse>>

    @POST("teams/{teamId}/members")
    suspend fun addTeamMember(@Path("teamId") teamId: String, @Query("userId") userId: String, @Query("role") role: String): Response<TeamMemberResponse>

    @DELETE("teams/{teamId}/members/{userId}")
    suspend fun removeTeamMember(@Path("teamId") teamId: String, @Path("userId") userId: String): Response<Unit>

    @PUT("teams/{teamId}/members/{userId}/role")
    suspend fun updateTeamMemberRole(@Path("teamId") teamId: String, @Path("userId") userId: String, @Query("role") role: String): Response<TeamMemberResponse>

    // Users
    @GET("users")
    suspend fun getUsers(): Response<List<UserResponse>>

    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: String): Response<UserResponse>

    @POST("users")
    suspend fun createUser(@Body request: UserRequest): Response<UserResponse>

    @PUT("users/{id}")
    suspend fun updateUser(
        @Path("id") id: String,
        @Body request: UserRequest
    ): Response<UserResponse>

    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") id: String): Response<Unit>

    // Kanban
    // Kanban board functionality will be implemented later

    // Chat
    @GET("teams/{team_id}/chat")
    suspend fun getTeamChat(
        @Path("team_id") teamId: Long,
        @Query("before") before: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<ChatResponse>

    @POST("teams/{team_id}/chat")
    @Multipart
    suspend fun sendTeamMessage(
        @Path("team_id") teamId: Long,
        @Part("content") content: String,
        @Part attachments: List<MultipartBody.Part>? = null
    ): Response<MessageResponse>

    @PUT("teams/{team_id}/chat/{message_id}")
    suspend fun updateMessage(
        @Path("team_id") teamId: Long,
        @Path("message_id") messageId: Long,
        @Body message: MessageRequest
    ): Response<MessageResponse>

    @DELETE("teams/{team_id}/chat/{message_id}")
    suspend fun deleteMessage(
        @Path("team_id") teamId: Long,
        @Path("message_id") messageId: Long
    ): Response<Unit>

    // Sync
    @GET("sync")
    suspend fun getSyncData(
        @Query("last_sync_at") lastSyncAt: String,
        @Query("device_id") deviceId: String
    ): Response<SyncResponse>

    @POST("sync")
    suspend fun syncData(@Body request: SyncRequest): Response<SyncResponse>

    // Notifications
    @GET("notifications")
    suspend fun getNotifications(@Query("since") since: Long? = null): Response<List<NotificationResponse>>

    @POST("notifications/register")
    suspend fun registerDevice(@Body request: RegisterDeviceRequest): Response<Unit>

    @DELETE("notifications/register/{device_id}")
    suspend fun unregisterDevice(@Path("device_id") deviceId: String): Response<Unit>

    @PATCH("notifications/settings")
    suspend fun updateNotificationSettings(@Body request: NotificationSettingsRequest): Response<NotificationSettingsResponse>

    // Analytics
    @GET("analytics/tasks")
    suspend fun getTaskAnalytics(
        @Query("team_id") teamId: Long? = null,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String
    ): Response<TaskAnalyticsResponse>

    @GET("analytics/teams/{team_id}/performance")
    suspend fun getTeamPerformance(
        @Path("team_id") teamId: Long,
        @Query("period") period: String,
        @Query("start_date") startDate: String
    ): Response<TeamPerformanceResponse>

    // Calendar
    @GET("calendar/events")
    suspend fun getCalendarEvents(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("team_id") teamId: Long? = null
    ): Response<List<CalendarEventResponse>>

    @POST("calendar/events")
    suspend fun createCalendarEvent(@Body request: CalendarEventRequest): Response<CalendarEventResponse>
}