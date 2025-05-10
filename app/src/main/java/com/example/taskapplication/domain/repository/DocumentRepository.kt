package com.example.taskapplication.domain.repository

import com.example.taskapplication.domain.model.Document
import com.example.taskapplication.domain.model.DocumentFolder
import com.example.taskapplication.domain.model.DocumentPermission
import com.example.taskapplication.domain.model.DocumentVersion
import com.example.taskapplication.util.Resource
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Repository interface for document operations
 */
interface DocumentRepository {
    
    // Document Folders
    
    /**
     * Get all folders for a team
     */
    fun getFoldersByTeam(teamId: String): Flow<Resource<List<DocumentFolder>>>
    
    /**
     * Get subfolders for a folder
     */
    fun getSubfolders(folderId: String): Flow<Resource<List<DocumentFolder>>>
    
    /**
     * Get root folders for a team
     */
    fun getRootFolders(teamId: String): Flow<Resource<List<DocumentFolder>>>
    
    /**
     * Get folder by ID
     */
    suspend fun getFolderById(folderId: String): Resource<DocumentFolder>
    
    /**
     * Create a new folder
     */
    suspend fun createFolder(folder: DocumentFolder): Resource<DocumentFolder>
    
    /**
     * Update a folder
     */
    suspend fun updateFolder(folder: DocumentFolder): Resource<DocumentFolder>
    
    /**
     * Delete a folder
     */
    suspend fun deleteFolder(folderId: String): Resource<Unit>
    
    // Documents
    
    /**
     * Get all documents for a team
     */
    fun getDocumentsByTeam(teamId: String): Flow<Resource<List<Document>>>
    
    /**
     * Get documents in a folder
     */
    fun getDocumentsByFolder(folderId: String): Flow<Resource<List<Document>>>
    
    /**
     * Get root documents for a team (not in any folder)
     */
    fun getRootDocuments(teamId: String): Flow<Resource<List<Document>>>
    
    /**
     * Get document by ID
     */
    suspend fun getDocumentById(documentId: String): Resource<Document>
    
    /**
     * Get document by ID as Flow
     */
    fun getDocumentByIdFlow(documentId: String): Flow<Resource<Document>>
    
    /**
     * Create a new document
     */
    suspend fun createDocument(document: Document, file: File): Resource<Document>
    
    /**
     * Update a document
     */
    suspend fun updateDocument(document: Document): Resource<Document>
    
    /**
     * Delete a document
     */
    suspend fun deleteDocument(documentId: String): Resource<Unit>
    
    /**
     * Download a document
     */
    suspend fun downloadDocument(documentId: String, versionId: String? = null): Resource<File>
    
    /**
     * Search documents by name or description
     */
    fun searchDocuments(teamId: String, query: String): Flow<Resource<List<Document>>>
    
    // Document Versions
    
    /**
     * Get all versions for a document
     */
    fun getVersionsByDocument(documentId: String): Flow<Resource<List<DocumentVersion>>>
    
    /**
     * Get version by ID
     */
    suspend fun getVersionById(versionId: String): Resource<DocumentVersion>
    
    /**
     * Get latest version for a document
     */
    suspend fun getLatestVersion(documentId: String): Resource<DocumentVersion>
    
    /**
     * Create a new version
     */
    suspend fun createVersion(version: DocumentVersion, file: File): Resource<DocumentVersion>
    
    // Document Permissions
    
    /**
     * Get all permissions for a document
     */
    fun getPermissionsByDocument(documentId: String): Flow<Resource<List<DocumentPermission>>>
    
    /**
     * Get permission for a user on a document
     */
    suspend fun getUserPermission(documentId: String, userId: String): Resource<DocumentPermission>
    
    /**
     * Create a new permission
     */
    suspend fun createPermission(permission: DocumentPermission): Resource<DocumentPermission>
    
    /**
     * Delete a permission
     */
    suspend fun deletePermission(documentId: String, userId: String): Resource<Unit>
    
    // Sync
    
    /**
     * Sync documents with server
     */
    suspend fun syncDocuments(): Resource<Unit>
}
