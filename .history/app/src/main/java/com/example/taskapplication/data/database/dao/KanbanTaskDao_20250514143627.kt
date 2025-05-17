package com.example.taskapplication.data.database.dao

import androidx.room.*
import com.example.taskapplication.data.database.entities.KanbanTaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for kanban tasks
 */
@Dao
interface KanbanTaskDao {
    /**
     * Get tasks by column
     */
    @Query("SELECT * FROM kanban_tasks WHERE columnId = :columnId ORDER BY position ASC")
    fun getTasksByColumn(columnId: String): Flow<List<KanbanTaskEntity>>

    /**
     * Get task by ID
     */
    @Query("SELECT * FROM kanban_tasks WHERE id = :id")
    suspend fun getTaskById(id: String): KanbanTaskEntity?

    /**
     * Insert a new task
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: KanbanTaskEntity)

    /**
     * Update a task
     */
    @Update
    suspend fun updateTask(task: KanbanTaskEntity)

    /**
     * Delete a task
     */
    @Delete
    suspend fun deleteTask(task: KanbanTaskEntity)

    /**
     * Get all pending sync tasks
     */
    @Query("SELECT * FROM kanban_tasks WHERE syncStatus IN ('pending_create', 'pending_update', 'pending_delete')")
    suspend fun getPendingSyncTasks(): List<KanbanTaskEntity>

    /**
     * Get task by server ID
     */
    @Query("SELECT * FROM kanban_tasks WHERE serverId = :serverId")
    suspend fun getTaskByServerId(serverId: String): KanbanTaskEntity?

    /**
     * Get tasks by column (synchronous version)
     */
    @Query("SELECT * FROM kanban_tasks WHERE columnId = :columnId ORDER BY position ASC")
    suspend fun getTasksByColumnSync(columnId: String): List<KanbanTaskEntity>

    /**
     * Update task positions
     */
    @Transaction
    suspend fun updateTaskPositions(tasks: List<KanbanTaskEntity>) {
        for (task in tasks) {
            updateTask(task)
        }
    }
}
