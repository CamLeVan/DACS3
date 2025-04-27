package com.example.taskapplication.data.api

import com.example.taskapplication.data.api.request.*
import com.example.taskapplication.data.api.response.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // Auth
    @POST("auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<AuthResponse>

    @POST("auth/register")
    suspend fun register(@Body registerRequest: RegisterRequest): Response<AuthResponse>

    @POST("auth/google")
    suspend fun loginWithGoogle(@Body googleAuthRequest: GoogleAuthRequest): Response<AuthResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    @GET("user")
    suspend fun getCurrentUser(): Response<UserResponse>

    // Personal Tasks
    @GET("personal-tasks")
    suspend fun getPersonalTasks(): Response<List<PersonalTaskResponse>>

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
    suspend fun getTeamMembers(@Path("id") id: Long): Response<List<UserResponse>>

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

    @PUT("team-tasks/{id}")
    suspend fun updateTeamTask(
        @Path("id") id: Long,
        @Body task: TeamTaskRequest
    ): Response<TeamTaskResponse>

    @DELETE("team-tasks/{id}")
    suspend fun deleteTeamTask(@Path("id") id: Long): Response<Unit>

    // Task Assignments
    @GET("teams/{team}/tasks/{task}/assignments")
    suspend fun getTaskAssignments(
        @Path("team") teamId: Long,
        @Path("task") taskId: Long
    ): Response<List<TaskAssignmentResponse>>

    @POST("teams/{team}/tasks/{task}/assignments")
    suspend fun createTaskAssignment(
        @Path("team") teamId: Long,
        @Path("task") taskId: Long,
        @Body request: TaskAssignmentRequest
    ): Response<TaskAssignmentResponse>

    @PUT("teams/{team}/tasks/{task}/assignments/{assignment}")
    suspend fun updateTaskAssignment(
        @Path("team") teamId: Long,
        @Path("task") taskId: Long,
        @Path("assignment") assignmentId: Long,
        @Body request: TaskAssignmentRequest
    ): Response<TaskAssignmentResponse>

    @DELETE("teams/{team}/tasks/{task}/assignments/{assignment}")
    suspend fun deleteTaskAssignment(
        @Path("team") teamId: Long,
        @Path("task") taskId: Long,
        @Path("assignment") assignmentId: Long
    ): Response<Unit>

    // Messages
    @GET("teams/{id}/messages")
    suspend fun getTeamMessages(
        @Path("id") teamId: Long,
        @Query("before") beforeTimestamp: Long? = null,
        @Query("limit") limit: Int = 50
    ): Response<List<MessageResponse>>

    @POST("teams/{id}/messages")
    suspend fun sendTeamMessage(
        @Path("id") teamId: Long,
        @Body message: MessageRequest
    ): Response<MessageResponse>

    @GET("messages/direct")
    suspend fun getDirectMessages(
        @Query("with") userId: Long,
        @Query("before") beforeTimestamp: Long? = null,
        @Query("limit") limit: Int = 50
    ): Response<List<MessageResponse>>

    @POST("messages/direct")
    suspend fun sendDirectMessage(@Body message: DirectMessageRequest): Response<MessageResponse>

    @PUT("messages/{id}")
    suspend fun updateMessage(
        @Path("id") id: Long,
        @Body message: MessageRequest
    ): Response<MessageResponse>

    @DELETE("messages/{id}")
    suspend fun deleteMessage(@Path("id") id: Long): Response<Unit>

    // Message Read Status
    @POST("messages/{id}/read")
    suspend fun markMessageAsRead(@Path("id") messageId: Long): Response<Unit>

    @GET("messages/unread-count")
    suspend fun getUnreadMessageCount(): Response<UnreadCountResponse>

    // Message Reactions
    @POST("messages/{id}/reactions")
    suspend fun addReaction(
        @Path("id") messageId: Long,
        @Body reaction: ReactionRequest
    ): Response<ReactionResponse>

    @DELETE("messages/{messageId}/reactions/{reactionId}")
    suspend fun removeReaction(
        @Path("messageId") messageId: Long,
        @Path("reactionId") reactionId: Long
    ): Response<Unit>

    // Sync
    @POST("sync/initial")
    suspend fun initialSync(@Body request: InitialSyncRequest): Response<InitialSyncResponse>

    @POST("sync/quick")
    suspend fun quickSync(@Body request: QuickSyncRequest): Response<QuickSyncResponse>

    @POST("sync/push")
    suspend fun pushChanges(@Body request: PushChangesRequest): Response<PushChangesResponse>
}