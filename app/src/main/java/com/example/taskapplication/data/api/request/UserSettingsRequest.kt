package com.example.taskapplication.data.api.request

import com.google.gson.annotations.SerializedName

data class UserSettingsRequest(
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
