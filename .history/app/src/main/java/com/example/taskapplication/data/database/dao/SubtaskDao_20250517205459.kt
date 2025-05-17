package com.example.taskapplication.data.database.dao

import androidx.room.*
import com.example.taskapplication.data.database.entities.SubtaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtaskDao {
    @Query("SELECT * FROM subtasks WHERE taskableId = :taskId ORDER BY `order` ASC")
    fun getSubtasksByTaskId(taskId: String): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtasks WHERE taskableId = :taskId ORDER BY `order` ASC")
    suspend fun getSubtasksByTaskIdSync(taskId: String): List<SubtaskEntity>

    @Query("SELECT * FROM subtasks WHERE id = :subtaskId")
    suspend fun getSubtaskById(subtaskId: String): SubtaskEntity?

    @Query("SELECT * FROM subtasks WHERE id IN (:subtaskIds)")
    suspend fun getSubtasksByIds(subtaskIds: List<String>): List<SubtaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtask(subtask: SubtaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtasks(subtasks: List<SubtaskEntity>)

    @Update
    suspend fun updateSubtask(subtask: SubtaskEntity)

    @Delete
    suspend fun deleteSubtask(subtask: SubtaskEntity)

    @Query("DELETE FROM subtasks WHERE id = :subtaskId")
    suspend fun deleteSubtask(subtaskId: String)

    @Query("DELETE FROM subtasks WHERE taskableId = :taskId")
    suspend fun deleteSubtasksByTaskId(taskId: String)

    @Query("UPDATE subtasks SET syncStatus = :syncStatus, lastModified = :timestamp WHERE id = :subtaskId")
    suspend fun updateSubtaskSyncStatus(subtaskId: String, syncStatus: String, timestamp: Long)

    @Query("UPDATE subtasks SET syncStatus = :syncStatus, lastModified = :timestamp WHERE taskableId = :taskId")
    suspend fun updateSubtasksSyncStatusByTaskId(taskId: String, syncStatus: String, timestamp: Long)

    @Query("SELECT MAX(`order`) FROM subtasks WHERE taskableId = :taskId")
    suspend fun getMaxOrderByTaskId(taskId: String): Int?

    @Query("UPDATE subtasks SET `order` = :newOrder, syncStatus = :updatedStatus, lastModified = :timestamp WHERE id = :subtaskId")
    suspend fun updateSubtaskOrder(subtaskId: String, newOrder: Int, updatedStatus: String, timestamp: Long)

    @Query("SELECT * FROM subtasks WHERE syncStatus = :syncStatus")
    suspend fun getSubtasksBySyncStatus(syncStatus: String): List<SubtaskEntity>

    @Query("UPDATE subtasks SET syncStatus = :deletedStatus, lastModified = :timestamp WHERE id = :subtaskId AND serverId IS NOT NULL")
    suspend fun markSubtaskForDeletion(subtaskId: String, timestamp: Long, deletedStatus: String = "pending_delete")

    @Query("DELETE FROM subtasks WHERE id = :subtaskId AND syncStatus = :createdStatus")
    suspend fun deleteLocalOnlySubtask(subtaskId: String, createdStatus: String = "pending_create")
}
