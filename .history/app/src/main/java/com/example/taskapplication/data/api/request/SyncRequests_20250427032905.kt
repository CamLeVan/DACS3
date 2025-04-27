package com.example.taskapplication.data.api.request

data class InitialSyncRequest(
    val deviceId: String
)

data class QuickSyncRequest(
    val lastSyncTimestamp: Long,
    val deviceId: String
)

data class PushChangesRequest(
    val deviceId: String,
    val personalTasks: List<PersonalTaskPushData>? = null,
    val teamTasks: List<TeamTaskPushData>? = null,
    val messageReadStatuses: List<MessageReadStatusPushData>? = null,
    val messageReactions: List<MessageReactionPushData>? = null
)

data class PersonalTaskPushData(
    val clientId: String,
    val serverId: String?,
    val action: String, // "create", "update", "delete"
    val data: PersonalTaskRequest?,
    val timestamp: Long
)

data class TeamTaskPushData(
    val clientId: String,
    val serverId: String?,
    val teamId: String,
    val action: String, // "create", "update", "delete"
    val data: TeamTaskRequest?,
    val timestamp: Long
)

data class MessageReadStatusPushData(
    val messageId: String,
    val timestamp: Long
)

data class MessageReactionPushData(
    val clientId: String?,
    val serverId: String?,
    val messageId: String,
    val action: String, // "add", "remove"
    val reaction: String?,
    val timestamp: Long
)