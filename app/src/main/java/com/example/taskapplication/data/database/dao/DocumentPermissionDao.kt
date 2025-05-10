package com.example.taskapplication.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.taskapplication.data.database.entities.DocumentPermissionEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for document permissions
 */
@Dao
interface DocumentPermissionDao {
    /**
     * Get all permissions for a document
     */
    @Query("SELECT * FROM document_permissions WHERE documentId = :documentId")
    fun getPermissionsByDocument(documentId: String): Flow<List<DocumentPermissionEntity>>
    
    /**
     * Get permission by ID
     */
    @Query("SELECT * FROM document_permissions WHERE id = :permissionId")
    suspend fun getPermissionById(permissionId: String): DocumentPermissionEntity?
    
    /**
     * Get permission for a user on a document
     */
    @Query("SELECT * FROM document_permissions WHERE documentId = :documentId AND userId = :userId")
    suspend fun getUserPermission(documentId: String, userId: String): DocumentPermissionEntity?
    
    /**
     * Insert a permission
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPermission(permission: DocumentPermissionEntity)
    
    /**
     * Insert multiple permissions
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPermissions(permissions: List<DocumentPermissionEntity>)
    
    /**
     * Delete a permission
     */
    @Query("DELETE FROM document_permissions WHERE documentId = :documentId AND userId = :userId")
    suspend fun deletePermission(documentId: String, userId: String)
    
    /**
     * Get permissions that need to be synced
     */
    @Query("SELECT * FROM document_permissions WHERE syncStatus = 'pending'")
    suspend fun getUnSyncedPermissions(): List<DocumentPermissionEntity>
    
    /**
     * Update permission sync status
     */
    @Query("UPDATE document_permissions SET syncStatus = :syncStatus, serverId = :serverId WHERE id = :permissionId")
    suspend fun updatePermissionSyncStatus(permissionId: String, syncStatus: String, serverId: String?)
}
