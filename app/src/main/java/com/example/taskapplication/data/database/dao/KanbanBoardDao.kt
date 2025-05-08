package com.example.taskapplication.data.database.dao

import androidx.room.*
import com.example.taskapplication.data.database.entities.KanbanBoardEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for kanban boards
 */
@Dao
interface KanbanBoardDao {
    /**
     * Get all kanban boards
     */
    @Query("SELECT * FROM kanban_boards ORDER BY name ASC")
    fun getAllBoards(): Flow<List<KanbanBoardEntity>>
    
    /**
     * Get kanban boards by team
     */
    @Query("SELECT * FROM kanban_boards WHERE teamId = :teamId ORDER BY name ASC")
    fun getBoardsByTeam(teamId: String): Flow<List<KanbanBoardEntity>>
    
    /**
     * Get kanban board by ID
     */
    @Query("SELECT * FROM kanban_boards WHERE id = :id")
    suspend fun getBoardById(id: String): KanbanBoardEntity?
    
    /**
     * Insert a new kanban board
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoard(board: KanbanBoardEntity)
    
    /**
     * Update a kanban board
     */
    @Update
    suspend fun updateBoard(board: KanbanBoardEntity)
    
    /**
     * Delete a kanban board
     */
    @Delete
    suspend fun deleteBoard(board: KanbanBoardEntity)
    
    /**
     * Get all pending sync boards
     */
    @Query("SELECT * FROM kanban_boards WHERE syncStatus IN ('pending_create', 'pending_update', 'pending_delete')")
    suspend fun getPendingSyncBoards(): List<KanbanBoardEntity>
    
    /**
     * Get board by server ID
     */
    @Query("SELECT * FROM kanban_boards WHERE serverId = :serverId")
    suspend fun getBoardByServerId(serverId: String): KanbanBoardEntity?
}
