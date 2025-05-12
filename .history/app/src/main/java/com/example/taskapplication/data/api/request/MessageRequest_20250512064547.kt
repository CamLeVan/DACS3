package com.example.taskapplication.data.api.request

import com.google.gson.annotations.SerializedName

data class MessageRequest(
    @SerializedName("message") val content: String,
    @SerializedName("team_id") val teamId: String? = null,
    @SerializedName("receiver_id") val receiverId: String? = null,
    @SerializedName("sender_id") val senderId: String? = null,
    @SerializedName("client_temp_id") val clientTempId: String? = null,
    @SerializedName("attachments") val attachments: List<AttachmentRequest>? = null
)