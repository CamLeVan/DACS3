package com.example.taskapplication.data.api.request

import com.google.gson.annotations.SerializedName

/**
 * Request model cho tệp đính kèm gửi lên API
 */
data class AttachmentRequest(
    @SerializedName("file_id") val fileId: String,
    @SerializedName("message_id") val messageId: String? = null
)
