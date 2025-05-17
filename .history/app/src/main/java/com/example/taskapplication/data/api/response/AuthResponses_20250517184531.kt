package com.example.taskapplication.data.api.response

data class AuthResponse(
    val user: UserResponse,
    val token: String,
    val device_id: String,
    val is_new_user: Boolean? = null
)

data class UserResponse(
    val id: Long,
    val name: String,
    val email: String,
    val avatar: String? = null,
    val google_id: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val settings: UserSettings? = null
)

data class UserSettings(
    val theme: String,
    val language: String,
    val notifications_enabled: Boolean,
    val task_reminders: Boolean,
    val team_invitations: Boolean,
    val team_updates: Boolean,
    val chat_messages: Boolean
)

data class UserDataResponse(
    val data: UserResponse
)

data class UserSettingsResponse(
    val data: UserSettings
)

data class MessageResponse(
    val message: String
)

data class SearchUsersResponse(
    val data: List<UserResponse>,
    val meta: PaginationMeta
)

data class PaginationMeta(
    val current_page: Int,
    val last_page: Int,
    val per_page: Int,
    val total: Int
)