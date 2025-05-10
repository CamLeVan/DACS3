package com.example.taskapplication.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response for resolving conflicts
 */
data class ResolveConflictsResponse(
    @SerializedName("resolved_documents") val resolvedDocuments: List<DocumentDto>,
    @SerializedName("resolved_folders") val resolvedFolders: List<DocumentFolderDto>,
    @SerializedName("resolved_versions") val resolvedVersions: List<DocumentVersionDto>,
    @SerializedName("resolved_permissions") val resolvedPermissions: List<DocumentPermissionDto>,
    @SerializedName("remaining_conflicts") val remainingConflicts: List<ConflictDto>
)
