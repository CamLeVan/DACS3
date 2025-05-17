package com.example.taskapplication.data.api.response

import com.google.gson.annotations.SerializedName

data class MessageResponse(
    @SerializedName("message")
    val message: String
)

data class ChatMessageResponse(
    @SerializedName("id")
    val id: Long,

    @SerializedName("message")
    val content: String,

    @SerializedName("team_id")
    val teamId: Long? = null,

    @SerializedName("user_id")
    val senderId: Long,

    @SerializedName("receiver_id")
    val receiverId: Long? = null,

    @SerializedName("created_at")
    val timestamp: Long,

    @SerializedName("updated_at")
    val lastModified: Long,

    @SerializedName("read_status")
    val readBy: List<Long> = emptyList(),

    @SerializedName("client_temp_id")
    val clientTempId: String? = null,

    @SerializedName("deleted_at")
    val deletedAt: Long? = null,

    @SerializedName("user")
    val user: UserResponse? = null
)