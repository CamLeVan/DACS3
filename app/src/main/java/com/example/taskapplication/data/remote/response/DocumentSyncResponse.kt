package com.example.taskapplication.data.remote.response

import com.example.taskapplication.data.remote.dto.DocumentDto
import com.example.taskapplication.data.remote.dto.DocumentFolderDto
import com.example.taskapplication.data.remote.dto.DocumentPermissionDto
import com.example.taskapplication.data.remote.dto.DocumentVersionDto
import com.google.gson.annotations.SerializedName

/**
 * Response for document sync
 */
data class DocumentSyncResponse(
    val folders: DocumentFolderSyncData,
    val documents: DocumentSyncData,
    val versions: DocumentVersionSyncData,
    val permissions: DocumentPermissionSyncData
)

/**
 * Sync data for document folders
 */
data class DocumentFolderSyncData(
    val created: List<DocumentFolderDto>,
    val updated: List<DocumentFolderDto>,
    @SerializedName("server_deleted_ids")
    val serverDeletedIds: List<String>
)

/**
 * Sync data for documents
 */
data class DocumentSyncData(
    val created: List<DocumentDto>,
    val updated: List<DocumentDto>,
    @SerializedName("server_deleted_ids")
    val serverDeletedIds: List<String>
)

/**
 * Sync data for document versions
 */
data class DocumentVersionSyncData(
    val created: List<DocumentVersionDto>,
    @SerializedName("server_deleted_ids")
    val serverDeletedIds: List<String>
)

/**
 * Sync data for document permissions
 */
data class DocumentPermissionSyncData(
    val created: List<DocumentPermissionDto>,
    @SerializedName("server_deleted_ids")
    val serverDeletedIds: List<String>
)
