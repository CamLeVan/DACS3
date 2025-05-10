package com.example.taskapplication.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.taskapplication.data.database.entities.DocumentFolderEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for document folders
 */
@Dao
interface DocumentFolderDao {
    /**
     * Get all folders for a team
     */
    @Query("SELECT * FROM document_folders WHERE teamId = :teamId AND isDeleted = 0 ORDER BY name ASC")
    suspend fun getFoldersByTeam(teamId: String): List<DocumentFolderEntity>

    /**
     * Get subfolders for a folder
     */
    @Query("SELECT * FROM document_folders WHERE parentFolderId = :folderId AND isDeleted = 0 ORDER BY name ASC")
    suspend fun getSubfolders(folderId: String): List<DocumentFolderEntity>

    /**
     * Get root folders for a team
     */
    @Query("SELECT * FROM document_folders WHERE teamId = :teamId AND parentFolderId IS NULL AND isDeleted = 0 ORDER BY name ASC")
    fun getRootFolders(teamId: String): Flow<List<DocumentFolderEntity>>

    /**
     * Get folder by ID
     */
    @Query("SELECT * FROM document_folders WHERE id = :folderId")
    suspend fun getFolderById(folderId: String): DocumentFolderEntity?

    /**
     * Insert a folder
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: DocumentFolderEntity)

    /**
     * Insert multiple folders
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<DocumentFolderEntity>)

    /**
     * Update a folder
     */
    @Update
    suspend fun updateFolder(folder: DocumentFolderEntity)

    /**
     * Mark a folder as deleted
     */
    @Query("UPDATE document_folders SET isDeleted = 1, syncStatus = 'pending', updatedAt = :timestamp WHERE id = :folderId")
    suspend fun markFolderAsDeleted(folderId: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Get folders that need to be synced
     */
    @Query("SELECT * FROM document_folders WHERE syncStatus = 'pending'")
    suspend fun getUnSyncedFolders(): List<DocumentFolderEntity>

    /**
     * Update folder sync status
     */
    @Query("UPDATE document_folders SET syncStatus = :syncStatus, serverId = :serverId WHERE id = :folderId")
    suspend fun updateFolderSyncStatus(folderId: String, syncStatus: String, serverId: String?)
}
