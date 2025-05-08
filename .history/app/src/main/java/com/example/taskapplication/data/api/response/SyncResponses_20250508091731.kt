package com.example.taskapplication.data.api.response

data class SyncResponse(
    val data: SyncData,
    val meta: SyncMeta
)

data class SyncData(
    val tasks: SyncDataResponse<TaskSyncResponse>,
    val messages: SyncDataResponse<MessageSyncResponse>
)

data class SyncDataResponse<T>(
    val created: List<T>,
    val updated: List<T>,
    val deleted: List<Long>
)

data class TaskSyncResponse(
    val local_id: String,
    val server_id: Long,
    val status: String
)

data class MessageSyncResponse(
    val local_id: String,
    val server_id: Long,
    val status: String
)

data class SyncMeta(
    val sync_timestamp: String
)

data class InitialSyncResponse(
    val user: UserResponse,
    val teams: List<TeamResponse>,
    val teamMembers: List<TeamMemberResponse>,
    val personalTasks: List<PersonalTaskResponse>,
    val teamTasks: List<TeamTaskResponse>,
    val messages: List<MessageResponse>?,
    val timestamp: Long
)

data class QuickSyncResponse(
    val users: List<UserResponse>?,
    val teams: Map<String, List<TeamResponse>>?, // "created", "updated", "deleted"
    val teamMembers: Map<String, List<TeamMemberResponse>>?, // "created", "updated", "deleted"
    val personalTasks: Map<String, List<PersonalTaskResponse>>?, // "created", "updated", "deleted"
    val teamTasks: Map<String, List<TeamTaskResponse>>?, // "created", "updated", "deleted"
    val messages: Map<String, List<MessageResponse>>?, // "created", "updated", "deleted"
    val timestamp: Long
)

data class PushChangesResponse(
    val success: Boolean,
    val conflicts: List<SyncConflict>?,
    val personalTasks: Map<String, Long>?, // Map client_id -> server_id for created tasks
    val teamTasks: Map<String, Long>?, // Map client_id -> server_id for created tasks
    val messageReactions: Map<String, Long>?, // Map client_id -> server_id for created reactions
    val timestamp: Long
)

data class SyncConflict(
    val entityType: String, // "personal_task", "team_task", etc.
    val clientId: String?,
    val serverId: Long?,
    val serverData: Any?, // The server's current data
    val action: String // "resolve_manually", "server_wins", "client_rejected"
) 