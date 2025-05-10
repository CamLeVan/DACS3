package com.example.taskapplication.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.taskapplication.data.database.entities.TeamDocumentEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for team documents
 */
@Dao
interface TeamDocumentDao {
    /**
     * Get all documents for a team
     */
    @Query("SELECT * FROM team_documents WHERE teamId = :teamId AND isDeleted = 0 ORDER BY lastModified DESC")
    fun getDocumentsByTeam(teamId: Long): Flow<List<TeamDocumentEntity>>

    /**
     * Get document by ID
     */
    @Query("SELECT * FROM team_documents WHERE id = :documentId")
    suspend fun getDocumentById(documentId: Long): TeamDocumentEntity?

    /**
     * Get documents by server ID
     */
    @Query("SELECT * FROM team_documents WHERE serverId = :serverId")
    suspend fun getDocumentByServerId(serverId: Long): TeamDocumentEntity?

    /**
     * Get documents uploaded by a user
     */
    @Query("SELECT * FROM team_documents WHERE uploadedBy = :userId AND isDeleted = 0 ORDER BY lastModified DESC")
    fun getDocumentsByUser(userId: Long): Flow<List<TeamDocumentEntity>>

    /**
     * Get documents by access level
     */
    @Query("SELECT * FROM team_documents WHERE teamId = :teamId AND accessLevel = :accessLevel AND isDeleted = 0 ORDER BY lastModified DESC")
    fun getDocumentsByAccessLevel(teamId: Long, accessLevel: String): Flow<List<TeamDocumentEntity>>

    /**
     * Get documents by file type
     */
    @Query("SELECT * FROM team_documents WHERE teamId = :teamId AND fileType = :fileType AND isDeleted = 0 ORDER BY lastModified DESC")
    fun getDocumentsByFileType(teamId: Long, fileType: String): Flow<List<TeamDocumentEntity>>

    /**
     * Search documents by name or description
     */
    @Query("SELECT * FROM team_documents WHERE teamId = :teamId AND (name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') AND isDeleted = 0 ORDER BY lastModified DESC")
    fun searchDocuments(teamId: Long, query: String): Flow<List<TeamDocumentEntity>>

    /**
     * Get documents that need to be synced
     */
    @Query("SELECT * FROM team_documents WHERE syncStatus != 'synced'")
    suspend fun getUnSyncedDocuments(): List<TeamDocumentEntity>

    /**
     * Insert a document
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: TeamDocumentEntity)

    /**
     * Insert multiple documents
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<TeamDocumentEntity>)

    /**
     * Update a document
     */
    @Update
    suspend fun updateDocument(document: TeamDocumentEntity)

    /**
     * Delete a document
     */
    @Delete
    suspend fun deleteDocument(document: TeamDocumentEntity)

    /**
     * Mark a document as deleted
     */
    @Query("UPDATE team_documents SET isDeleted = 1, lastModified = :timestamp WHERE id = :documentId")
    suspend fun markDocumentAsDeleted(documentId: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Update document sync status
     */
    @Query("UPDATE team_documents SET syncStatus = :syncStatus, serverId = :serverId, lastModified = :timestamp WHERE id = :documentId")
    suspend fun updateDocumentSyncStatus(documentId: String, syncStatus: String, serverId: String?, timestamp: Long = System.currentTimeMillis())

    /**
     * Update document access level
     */
    @Query("UPDATE team_documents SET accessLevel = :accessLevel, allowedUsers = :allowedUsers, lastModified = :timestamp WHERE id = :documentId")
    suspend fun updateDocumentAccessLevel(documentId: String, accessLevel: String, allowedUsers: String, timestamp: Long = System.currentTimeMillis())
}
