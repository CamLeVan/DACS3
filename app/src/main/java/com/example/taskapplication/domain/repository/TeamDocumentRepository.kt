package com.example.taskapplication.domain.repository

import com.example.taskapplication.domain.model.TeamDocument
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Repository for team documents
 */
interface TeamDocumentRepository {
    /**
     * Get all documents for a team
     */
    fun getDocumentsByTeam(teamId: String): Flow<List<TeamDocument>>
    
    /**
     * Get document by ID
     */
    suspend fun getDocumentById(documentId: String): TeamDocument?
    
    /**
     * Get documents uploaded by a user
     */
    fun getDocumentsByUser(userId: String): Flow<List<TeamDocument>>
    
    /**
     * Get documents by access level
     */
    fun getDocumentsByAccessLevel(teamId: String, accessLevel: String): Flow<List<TeamDocument>>
    
    /**
     * Get documents by file type
     */
    fun getDocumentsByFileType(teamId: String, fileType: String): Flow<List<TeamDocument>>
    
    /**
     * Search documents by name or description
     */
    fun searchDocuments(teamId: String, query: String): Flow<List<TeamDocument>>
    
    /**
     * Upload a document
     */
    suspend fun uploadDocument(
        teamId: String,
        name: String,
        description: String,
        file: File,
        accessLevel: String = "team",
        allowedUsers: List<String> = emptyList()
    ): Result<TeamDocument>
    
    /**
     * Update document details
     */
    suspend fun updateDocument(document: TeamDocument): Result<TeamDocument>
    
    /**
     * Delete a document
     */
    suspend fun deleteDocument(documentId: String): Result<Unit>
    
    /**
     * Download a document
     */
    suspend fun downloadDocument(documentId: String): Result<File>
    
    /**
     * Update document access level
     */
    suspend fun updateDocumentAccessLevel(
        documentId: String,
        accessLevel: String,
        allowedUsers: List<String> = emptyList()
    ): Result<TeamDocument>
    
    /**
     * Sync documents with server
     */
    suspend fun syncDocuments(): Result<Unit>
}
