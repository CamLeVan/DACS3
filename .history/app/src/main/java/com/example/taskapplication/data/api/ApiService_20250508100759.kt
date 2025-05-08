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
    suspend fun getPersonalTasks(
        @Query("status") status: String? = null,
        @Query("priority") priority: String? = null,
        @Query("due_date") dueDate: String? = null,
        @Query("search") search: String? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 15
    ): Response<PaginatedResponse<PersonalTaskResponse>>

    @POST("personal-tasks")
    suspend fun createPersonalTask(@Body task: PersonalTaskRequest): Response<PersonalTaskResponse>

    @PUT("personal-tasks/{id}")
    suspend fun updatePersonalTask(
        @Path("id") id: Long,
        @Body task: PersonalTaskRequest
    ): Response<PersonalTaskResponse>

    @DELETE("personal-tasks/{id}")
    suspend fun deletePersonalTask(@Path("id") id: Long): Response<Unit>

    // Teams
    @GET("teams")
    suspend fun getTeams(): Response<List<TeamResponse>>

    @POST("teams")
    suspend fun createTeam(@Body team: TeamRequest): Response<TeamResponse>

    @PUT("teams/{id}")
    suspend fun updateTeam(
        @Path("id") id: Long,
        @Body team: TeamRequest
    ): Response<TeamResponse>

    @DELETE("teams/{id}")
    suspend fun deleteTeam(@Path("id") id: Long): Response<Unit>

    @GET("teams/{id}/members")
    suspend fun getTeamMembers(@Path("id") id: Long): Response<List<TeamMemberResponse>>

    @POST("teams/{id}/members")
    suspend fun addTeamMember(
        @Path("id") id: Long,
        @Body request: AddTeamMemberRequest
    ): Response<Unit>

    @DELETE("teams/{teamId}/members/{userId}")
    suspend fun removeTeamMember(
        @Path("teamId") teamId: Long,
        @Path("userId") userId: Long
    ): Response<Unit>

    // Team Tasks
    @GET("teams/{id}/tasks")
    suspend fun getTeamTasks(@Path("id") teamId: Long): Response<List<TeamTaskResponse>>

    @POST("teams/{id}/tasks")
    suspend fun createTeamTask(
        @Path("id") teamId: Long,
        @Body task: TeamTaskRequest
    ): Response<TeamTaskResponse>

    @PUT("teams/{teamId}/tasks/{taskId}")
    suspend fun updateTeamTask(
        @Path("teamId") teamId: Long,
        @Path("taskId") taskId: Long,
        @Body task: TeamTaskRequest
    ): Response<TeamTaskResponse>

    @DELETE("teams/{teamId}/tasks/{taskId}")
    suspend fun deleteTeamTask(
        @Path("teamId") teamId: Long,
        @Path("taskId") taskId: Long
    ): Response<Unit>

    // Kanban
    @GET("teams/{team_id}/kanban")
    suspend fun getKanbanBoard(@Path("team_id") teamId: Long): Response<KanbanBoardResponse>

    @PATCH("teams/{team_id}/kanban/tasks/{task_id}/move")
    suspend fun moveTask(
        @Path("team_id") teamId: Long,
        @Path("task_id") taskId: Long,
        @Body request: MoveTaskRequest
    ): Response<TaskMoveResponse>

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