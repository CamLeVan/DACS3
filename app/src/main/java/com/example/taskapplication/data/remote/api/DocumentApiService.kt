package com.example.taskapplication.data.remote.api

import com.example.taskapplication.data.remote.dto.DocumentDto
import com.example.taskapplication.data.remote.dto.DocumentFolderDto
import com.example.taskapplication.data.remote.dto.DocumentPermissionDto
import com.example.taskapplication.data.remote.dto.DocumentVersionDto
import com.example.taskapplication.data.remote.request.CreateDocumentFolderRequest
import com.example.taskapplication.data.remote.request.CreateDocumentPermissionRequest
import com.example.taskapplication.data.remote.request.CreateDocumentRequest
import com.example.taskapplication.data.remote.request.CreateDocumentVersionRequest
import com.example.taskapplication.data.remote.request.UpdateDocumentFolderRequest
import com.example.taskapplication.data.remote.request.UpdateDocumentRequest
import com.example.taskapplication.data.remote.response.ApiResponse
import com.example.taskapplication.data.remote.response.DocumentSyncResponse
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/**
 * API service for document operations
 */
interface DocumentApiService {
    
    // Document Folders
    
    /**
     * Get all folders for a team
     */
    @GET("teams/{teamId}/document-folders")
    suspend fun getFoldersByTeam(
        @Path("teamId") teamId: String
    ): ApiResponse<List<DocumentFolderDto>>
    
    /**
     * Get folder by ID
     */
    @GET("document-folders/{folderId}")
    suspend fun getFolderById(
        @Path("folderId") folderId: String
    ): ApiResponse<DocumentFolderDto>
    
    /**
     * Create a new folder
     */
    @POST("document-folders")
    suspend fun createFolder(
        @Body request: CreateDocumentFolderRequest
    ): ApiResponse<DocumentFolderDto>
    
    /**
     * Update a folder
     */
    @PUT("document-folders/{folderId}")
    suspend fun updateFolder(
        @Path("folderId") folderId: String,
        @Body request: UpdateDocumentFolderRequest
    ): ApiResponse<DocumentFolderDto>
    
    /**
     * Delete a folder
     */
    @DELETE("document-folders/{folderId}")
    suspend fun deleteFolder(
        @Path("folderId") folderId: String
    ): ApiResponse<Unit>
    
    // Documents
    
    /**
     * Get all documents for a team
     */
    @GET("teams/{teamId}/documents")
    suspend fun getDocumentsByTeam(
        @Path("teamId") teamId: String,
        @Query("folderId") folderId: String? = null
    ): ApiResponse<List<DocumentDto>>
    
    /**
     * Get document by ID
     */
    @GET("documents/{documentId}")
    suspend fun getDocumentById(
        @Path("documentId") documentId: String
    ): ApiResponse<DocumentDto>
    
    /**
     * Create a new document
     */
    @Multipart
    @POST("documents")
    suspend fun createDocument(
        @Part("data") request: CreateDocumentRequest,
        @Part file: MultipartBody.Part
    ): ApiResponse<DocumentDto>
    
    /**
     * Update a document
     */
    @PUT("documents/{documentId}")
    suspend fun updateDocument(
        @Path("documentId") documentId: String,
        @Body request: UpdateDocumentRequest
    ): ApiResponse<DocumentDto>
    
    /**
     * Delete a document
     */
    @DELETE("documents/{documentId}")
    suspend fun deleteDocument(
        @Path("documentId") documentId: String
    ): ApiResponse<Unit>
    
    /**
     * Download a document
     */
    @Streaming
    @GET("documents/{documentId}/download")
    suspend fun downloadDocument(
        @Path("documentId") documentId: String,
        @Query("versionId") versionId: String? = null
    ): Response<ResponseBody>
    
    // Document Versions
    
    /**
     * Get all versions for a document
     */
    @GET("documents/{documentId}/versions")
    suspend fun getVersionsByDocument(
        @Path("documentId") documentId: String
    ): ApiResponse<List<DocumentVersionDto>>
    
    /**
     * Create a new version
     */
    @Multipart
    @POST("documents/{documentId}/versions")
    suspend fun createVersion(
        @Path("documentId") documentId: String,
        @Part("data") request: CreateDocumentVersionRequest,
        @Part file: MultipartBody.Part
    ): ApiResponse<DocumentVersionDto>
    
    // Document Permissions
    
    /**
     * Get all permissions for a document
     */
    @GET("documents/{documentId}/permissions")
    suspend fun getPermissionsByDocument(
        @Path("documentId") documentId: String
    ): ApiResponse<List<DocumentPermissionDto>>
    
    /**
     * Create a new permission
     */
    @POST("documents/{documentId}/permissions")
    suspend fun createPermission(
        @Path("documentId") documentId: String,
        @Body request: CreateDocumentPermissionRequest
    ): ApiResponse<DocumentPermissionDto>
    
    /**
     * Delete a permission
     */
    @DELETE("documents/{documentId}/permissions/{userId}")
    suspend fun deletePermission(
        @Path("documentId") documentId: String,
        @Path("userId") userId: String
    ): ApiResponse<Unit>
    
    // Sync
    
    /**
     * Sync documents
     */
    @POST("documents/sync")
    suspend fun syncDocuments(
        @Body data: Map<String, Any>
    ): ApiResponse<DocumentSyncResponse>
}
