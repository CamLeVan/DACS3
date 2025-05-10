package com.example.taskapplication.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.taskapplication.data.database.entities.DocumentEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for documents
 */
@Dao
interface DocumentDao {
    /**
     * Get all documents for a team
     */
    @Query("SELECT * FROM documents WHERE teamId = :teamId AND isDeleted = 0 ORDER BY name ASC")
    fun getDocumentsByTeam(teamId: String): Flow<List<DocumentEntity>>
    
    /**
     * Get documents in a folder
     */
    @Query("SELECT * FROM documents WHERE folderId = :folderId AND isDeleted = 0 ORDER BY name ASC")
    fun getDocumentsByFolder(folderId: String): Flow<List<DocumentEntity>>
    
    /**
     * Get root documents for a team (not in any folder)
     */
    @Query("SELECT * FROM documents WHERE teamId = :teamId AND folderId IS NULL AND isDeleted = 0 ORDER BY name ASC")
    fun getRootDocuments(teamId: String): Flow<List<DocumentEntity>>
    
    /**
     * Get document by ID
     */
    @Query("SELECT * FROM documents WHERE id = :documentId")
    suspend fun getDocumentById(documentId: String): DocumentEntity?
    
    /**
     * Get document by ID as Flow
     */
    @Query("SELECT * FROM documents WHERE id = :documentId")
    fun getDocumentByIdFlow(documentId: String): Flow<DocumentEntity?>
    
    /**
     * Insert a document
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)
    
    /**
     * Insert multiple documents
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<DocumentEntity>)
    
    /**
     * Update a document
     */
    @Update
    suspend fun updateDocument(document: DocumentEntity)
    
    /**
     * Mark a document as deleted
     */
    @Query("UPDATE documents SET isDeleted = 1, syncStatus = 'pending', lastModified = :timestamp WHERE id = :documentId")
    suspend fun markDocumentAsDeleted(documentId: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * Get documents that need to be synced
     */
    @Query("SELECT * FROM documents WHERE syncStatus = 'pending'")
    suspend fun getUnSyncedDocuments(): List<DocumentEntity>
    
    /**
     * Update document sync status
     */
    @Query("UPDATE documents SET syncStatus = :syncStatus, serverId = :serverId WHERE id = :documentId")
    suspend fun updateDocumentSyncStatus(documentId: String, syncStatus: String, serverId: String?)
    
    /**
     * Search documents by name or description
     */
    @Query("SELECT * FROM documents WHERE teamId = :teamId AND isDeleted = 0 AND (name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchDocuments(teamId: String, query: String): Flow<List<DocumentEntity>>
    
    /**
     * Update document latest version
     */
    @Query("UPDATE documents SET latestVersionId = :versionId, lastModified = :timestamp, syncStatus = 'pending' WHERE id = :documentId")
    suspend fun updateDocumentLatestVersion(documentId: String, versionId: String, timestamp: Long = System.currentTimeMillis())
}
