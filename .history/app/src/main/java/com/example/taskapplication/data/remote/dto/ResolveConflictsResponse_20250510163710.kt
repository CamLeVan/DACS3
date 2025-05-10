package com.example.taskapplication.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response for resolving conflicts
 */
data class ResolveConflictsResponse(
    @SerializedName("resolved") val resolved: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("documents") val documents: List<DocumentDto>?
)
