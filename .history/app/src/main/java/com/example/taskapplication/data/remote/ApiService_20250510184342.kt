package com.example.taskapplication.data.remote

import com.example.taskapplication.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * API service interface for document operations
 */
interface ApiService {

    // Document Endpoints

    /**
     * Get documents for a team
     */
    @GET("api/teams/{teamId}/documents")
    suspend fun getTeamDocuments(
        @Path("teamId") teamId: String,
        @Query("folder_id") folderId: String? = null,
        @Query("search") search: String? = null,
        @Query("file_type") fileType: String? = null,
        @Query("sort_by") sortBy: String? = null,
        @Query("sort_direction") sortDirection: String? = null,
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null
    ): Response<ApiResponse<List<DocumentDto>>>

    /**
     * Upload a new document
     */
    @Multipart
    @POST("api/teams/{teamId}/documents")
    suspend fun uploadDocument(
        @Path("teamId") teamId: String,
        @Part file: MultipartBody.Part,
        @Part("name") name: RequestBody? = null,
        @Part("description") description: RequestBody? = null,
        @Part("folder_id") folderId: RequestBody? = null,
        @Part("access_level") accessLevel: RequestBody? = null,
        @Part("allowed_users") allowedUsers: RequestBody? = null
    ): Response<ApiResponse<DocumentDto>>

    /**
     * Get document details
     */
    @GET("api/documents/{documentId}")
    suspend fun getDocument(
        @Path("documentId") documentId: String
    ): Response<ApiResponse<DocumentDto>>

    /**
     * Update document
     */
    @PUT("api/documents/{documentId}")
    suspend fun updateDocument(
        @Path("documentId") documentId: String,
        @Body request: UpdateDocumentRequest
    ): Response<ApiResponse<DocumentDto>>

    /**
     * Delete document
     */
    @DELETE("api/documents/{documentId}")
    suspend fun deleteDocument(
        @Path("documentId") documentId: String
    ): Response<ApiResponse<MessageResponse>>

    /**
     * Download document
     */
    @Streaming
    @GET("api/documents/{documentId}/download")
    suspend fun downloadDocument(
        @Path("documentId") documentId: String
    ): Response<ResponseBody>

    /**
     * Update document access
     */
    @PUT("api/documents/{documentId}/access")
    suspend fun updateDocumentAccess(
        @Path("documentId") documentId: String,
        @Body request: UpdateDocumentAccessRequest
    ): Response<ApiResponse<DocumentPermissionDto>>

    // Folder Endpoints

    /**
     * Get folders for a team
     */
    @GET("api/teams/{teamId}/folders")
    suspend fun getTeamFolders(
        @Path("teamId") teamId: String,
        @Query("parent_id") parentId: String? = null
    ): Response<ApiResponse<List<DocumentFolderDto>>>

    /**
     * Create a new folder
     */
    @POST("api/teams/{teamId}/folders")
    suspend fun createFolder(
        @Path("teamId") teamId: String,
        @Body request: CreateFolderRequest
    ): Response<ApiResponse<FolderDto>>

    /**
     * Get folder details
     */
    @GET("api/folders/{folderId}")
    suspend fun getFolder(
        @Path("folderId") folderId: String
    ): Response<ApiResponse<FolderDto>>

    /**
     * Update folder
     */
    @PUT("api/folders/{folderId}")
    suspend fun updateFolder(
        @Path("folderId") folderId: String,
        @Body request: UpdateFolderRequest
    ): Response<ApiResponse<FolderDto>>

    /**
     * Delete folder
     */
    @DELETE("api/folders/{folderId}")
    suspend fun deleteFolder(
        @Path("folderId") folderId: String,
        @Query("delete_contents") deleteContents: Boolean? = null
    ): Response<ApiResponse<MessageResponse>>

    // Version Endpoints

    /**
     * Get versions for a document
     */
    @GET("api/documents/{documentId}/versions")
    suspend fun getDocumentVersions(
        @Path("documentId") documentId: String
    ): Response<ApiResponse<List<VersionDto>>>

    /**
     * Upload a new version
     */
    @Multipart
    @POST("api/documents/{documentId}/versions")
    suspend fun uploadVersion(
        @Path("documentId") documentId: String,
        @Part file: MultipartBody.Part,
        @Part("version_note") versionNote: RequestBody? = null
    ): Response<ApiResponse<VersionDto>>

    /**
     * Get version details
     */
    @GET("api/documents/{documentId}/versions/{versionNumber}")
    suspend fun getVersion(
        @Path("documentId") documentId: String,
        @Path("versionNumber") versionNumber: Int
    ): Response<ApiResponse<VersionDto>>

    /**
     * Download specific version
     */
    @Streaming
    @GET("api/documents/{documentId}/versions/{versionNumber}/download")
    suspend fun downloadVersion(
        @Path("documentId") documentId: String,
        @Path("versionNumber") versionNumber: Int
    ): Response<ResponseBody>

    /**
     * Restore version
     */
    @POST("api/documents/{documentId}/versions/{versionNumber}/restore")
    suspend fun restoreVersion(
        @Path("documentId") documentId: String,
        @Path("versionNumber") versionNumber: Int
    ): Response<ApiResponse<DocumentDto>>

    // Sync Endpoints

    /**
     * Get changed documents
     */
    @GET("api/sync/documents")
    suspend fun getChangedDocuments(
        @Query("last_sync_at") lastSyncAt: String,
        @Query("team_id") teamId: String? = null
    ): Response<ApiResponse<SyncResponse>>

    /**
     * Resolve conflicts
     */
    @POST("api/sync/documents/resolve-conflicts")
    suspend fun resolveConflicts(
        @Body request: ResolveConflictsRequest
    ): Response<ApiResponse<ResolveConflictsResponse>>
}
