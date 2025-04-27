package com.example.taskapplication.data.api.response

data class FileUploadResponse(
    val id: Long,
    val url: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val uploadedAt: Long
)
