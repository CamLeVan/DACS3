package com.example.taskapplication.data.api.response

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("user")
    val user: UserResponse,

    @SerializedName("token")
    val token: String,

    @SerializedName("device_id")
    val deviceId: String,

    @SerializedName("is_new_user")
    val isNewUser: Boolean? = null
)

data class UserResponse(
    @SerializedName("id")
    val id: Long,

    @SerializedName("name")
    val name: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("avatar")
    val avatar: String? = null,

    @SerializedName("google_id")
    val googleId: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("updated_at")
    val updatedAt: String? = null,

    @SerializedName("settings")
    val settings: UserSettings? = null
)

data class UserSettings(
    @SerializedName("theme")
    val theme: String,

    @SerializedName("language")
    val language: String,

    @SerializedName("notifications_enabled")
    val notificationsEnabled: Boolean,

    @SerializedName("task_reminders")
    val taskReminders: Boolean,

    @SerializedName("team_invitations")
    val teamInvitations: Boolean,

    @SerializedName("team_updates")
    val teamUpdates: Boolean,

    @SerializedName("chat_messages")
    val chatMessages: Boolean
)

data class UserDataResponse(
    @SerializedName("data")
    val data: UserResponse
)

data class UserSettingsResponse(
    @SerializedName("data")
    val data: UserSettings
)