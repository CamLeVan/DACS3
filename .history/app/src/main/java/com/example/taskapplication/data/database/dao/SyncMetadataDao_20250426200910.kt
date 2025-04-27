package com.example.taskapplication.data.database.dao

import androidx.room.*
import com.example.taskapplication.data.database.entities.SyncMetadataEntity

@Dao
interface SyncMetadataDao {
    @Query("SELECT * FROM sync_metadata WHERE id = :id")
    suspend fun getSyncMetadata(id: String): SyncMetadataEntity?

    @Query("SELECT * FROM sync_metadata WHERE entityType = :entityType")
    suspend fun getSyncMetadataByType(entityType: String): List<SyncMetadataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncMetadata(metadata: SyncMetadataEntity)

    @Update
    suspend fun updateSyncMetadata(metadata: SyncMetadataEntity)

    @Query("UPDATE sync_metadata SET lastSyncTimestamp = :timestamp WHERE id = :id")
    suspend fun updateSyncTimestamp(id: String, timestamp: Long)

    @Query("UPDATE sync_metadata SET lastSyncTimestamp = :timestamp WHERE entityType = :entityType")
    suspend fun updateSyncTimestampByType(entityType: String, timestamp: Long)

    @Delete
    suspend fun deleteSyncMetadata(metadata: SyncMetadataEntity)

    @Query("DELETE FROM sync_metadata WHERE entityType = :entityType")
    suspend fun deleteSyncMetadataByType(entityType: String)

    // Helper method to get the last sync timestamp for global sync
    @Query("SELECT lastSyncTimestamp FROM sync_metadata WHERE id = 'global'")
    suspend fun getLastGlobalSyncTimestamp(): Long?

    // Update global sync timestamp
    @Query("INSERT OR REPLACE INTO sync_metadata (id, lastSyncTimestamp, entityType) VALUES ('global', :timestamp, 'global')")
    suspend fun updateGlobalSyncTimestamp(timestamp: Long)
} 