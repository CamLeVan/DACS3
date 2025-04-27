package com.example.taskapplication.data.database.dao

import androidx.room.*
import com.example.taskapplication.data.database.entities.PersonalTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalTaskDao {
    @Query("SELECT * FROM personal_tasks ORDER BY dueDate ASC")
    fun getAllTasks(): Flow<List<PersonalTaskEntity>>

    @Query("SELECT * FROM personal_tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): PersonalTaskEntity?

    @Query("SELECT * FROM personal_tasks WHERE syncStatus != 'synced'")
    suspend fun getPendingSyncTasks(): List<PersonalTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: PersonalTaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<PersonalTaskEntity>) // For batch insert/update from sync

    @Update
    suspend fun updateTask(task: PersonalTaskEntity)

    @Delete
    suspend fun deleteTask(task: PersonalTaskEntity)

    // Mark for deletion when user deletes a synced task
    @Query("UPDATE personal_tasks SET syncStatus = 'pending_delete', lastModified = :timestamp WHERE id = :taskId AND serverId IS NOT NULL")
    suspend fun markTaskForDeletion(taskId: String, timestamp: Long)

    // Delete a task that was created locally and never synced
    @Query("DELETE FROM personal_tasks WHERE id = :taskId AND syncStatus = 'pending_create'")
    suspend fun deleteLocalOnlyTask(taskId: String)

    // Delete task from local DB after server confirms deletion during sync
    @Query("DELETE FROM personal_tasks WHERE id = :taskId")
    suspend fun deleteSyncedTask(taskId: String)

    // Clear tasks marked for deletion (e.g., after successful sync push)
    @Query("DELETE FROM personal_tasks WHERE syncStatus = 'pending_delete'")
    suspend fun clearDeletedTasks()
} 