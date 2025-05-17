package com.example.taskapplication.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtask(subtask: SubtaskEntity)
    
    @Update
    suspend fun updateSubtask(subtask: SubtaskEntity)
    
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
    
    @Query("UPDATE subtasks SET `order` = :newOrder WHERE id = :subtaskId")
    suspend fun updateSubtaskOrder(subtaskId: String, newOrder: Int)
    
    @Query("SELECT * FROM subtasks WHERE syncStatus = :syncStatus")
    suspend fun getSubtasksBySyncStatus(syncStatus: String): List<SubtaskEntity>
}
