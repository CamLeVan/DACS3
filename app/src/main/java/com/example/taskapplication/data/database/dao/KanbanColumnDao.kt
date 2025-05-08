package com.example.taskapplication.data.database.dao

import androidx.room.*
import com.example.taskapplication.data.database.entities.KanbanColumnEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for kanban columns
 */
@Dao
interface KanbanColumnDao {
    /**
     * Get columns by board
     */
    @Query("SELECT * FROM kanban_columns WHERE boardId = :boardId ORDER BY `order` ASC")
    fun getColumnsByBoard(boardId: String): Flow<List<KanbanColumnEntity>>
    
    /**
     * Get column by ID
     */
    @Query("SELECT * FROM kanban_columns WHERE id = :id")
    suspend fun getColumnById(id: String): KanbanColumnEntity?
    
    /**
     * Insert a new column
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertColumn(column: KanbanColumnEntity)
    
    /**
     * Update a column
     */
    @Update
    suspend fun updateColumn(column: KanbanColumnEntity)
    
    /**
     * Delete a column
     */
    @Delete
    suspend fun deleteColumn(column: KanbanColumnEntity)
    
    /**
     * Get all pending sync columns
     */
    @Query("SELECT * FROM kanban_columns WHERE syncStatus IN ('pending_create', 'pending_update', 'pending_delete')")
    suspend fun getPendingSyncColumns(): List<KanbanColumnEntity>
    
    /**
     * Get column by server ID
     */
    @Query("SELECT * FROM kanban_columns WHERE serverId = :serverId")
    suspend fun getColumnByServerId(serverId: String): KanbanColumnEntity?
}
