package com.example.taskapplication.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.taskapplication.data.database.entities.DocumentVersionEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for document versions
 */
@Dao
interface DocumentVersionDao {
    /**
     * Get all versions for a document
     */
    @Query("SELECT * FROM document_versions WHERE documentId = :documentId ORDER BY versionNumber DESC")
    fun getVersionsByDocument(documentId: String): Flow<List<DocumentVersionEntity>>
    
    /**
     * Get version by ID
     */
    @Query("SELECT * FROM document_versions WHERE id = :versionId")
    suspend fun getVersionById(versionId: String): DocumentVersionEntity?
    
    /**
     * Get latest version for a document
     */
    @Query("SELECT * FROM document_versions WHERE documentId = :documentId ORDER BY versionNumber DESC LIMIT 1")
    suspend fun getLatestVersion(documentId: String): DocumentVersionEntity?
    
    /**
     * Insert a version
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: DocumentVersionEntity)
    
    /**
     * Insert multiple versions
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersions(versions: List<DocumentVersionEntity>)
    
    /**
     * Get versions that need to be synced
     */
    @Query("SELECT * FROM document_versions WHERE syncStatus = 'pending'")
    suspend fun getUnSyncedVersions(): List<DocumentVersionEntity>
    
    /**
     * Update version sync status
     */
    @Query("UPDATE document_versions SET syncStatus = :syncStatus, serverId = :serverId WHERE id = :versionId")
    suspend fun updateVersionSyncStatus(versionId: String, syncStatus: String, serverId: String?)
    
    /**
     * Get next version number for a document
     */
    @Query("SELECT COALESCE(MAX(versionNumber), 0) + 1 FROM document_versions WHERE documentId = :documentId")
    suspend fun getNextVersionNumber(documentId: String): Int
}
