package com.example.taskapplication.data.remote.request

import com.google.gson.annotations.SerializedName

/**
 * Request to create a document folder
 */
data class CreateDocumentFolderRequest(
    val name: String,
    val description: String,
    @SerializedName("team_id")
    val teamId: String,
    @SerializedName("parent_folder_id")
    val parentFolderId: String? = null
)

/**
 * Request to update a document folder
 */
data class UpdateDocumentFolderRequest(
    val name: String? = null,
    val description: String? = null,
    @SerializedName("parent_folder_id")
    val parentFolderId: String? = null
)

/**
 * Request to create a document
 */
data class CreateDocumentRequest(
    val name: String,
    val description: String,
    @SerializedName("team_id")
    val teamId: String,
    @SerializedName("folder_id")
    val folderId: String? = null,
    @SerializedName("access_level")
    val accessLevel: String = "team", // public, team, private, specific_users
    @SerializedName("allowed_users")
    val allowedUsers: List<String> = emptyList()
)

/**
 * Request to update a document
 */
data class UpdateDocumentRequest(
    val name: String? = null,
    val description: String? = null,
    @SerializedName("folder_id")
    val folderId: String? = null,
    @SerializedName("access_level")
    val accessLevel: String? = null,
    @SerializedName("allowed_users")
    val allowedUsers: List<String>? = null
)

/**
 * Request to create a document version
 */
data class CreateDocumentVersionRequest(
    @SerializedName("change_notes")
    val changeNotes: String
)

/**
 * Request to create a document permission
 */
data class CreateDocumentPermissionRequest(
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("permission_type")
    val permissionType: String // view, edit, admin
)
