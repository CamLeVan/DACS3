package com.example.taskapplication.data.database.dao

import androidx.room.*
import com.example.taskapplication.data.database.entities.UserInteractionEntity

@Dao
interface UserInteractionDao {
    @Query("SELECT * FROM user_interactions WHERE user_id = :userId")
    suspend fun getUserInteractions(userId: String): List<UserInteractionEntity>
    
    @Query("SELECT * FROM user_interactions WHERE user_id = :userId AND interaction_type = :type")
    suspend fun getUserInteractionsByType(userId: String, type: String): List<UserInteractionEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserInteraction(interaction: UserInteractionEntity): Long
    
    @Update
    suspend fun updateUserInteraction(interaction: UserInteractionEntity)
    
    @Query("UPDATE user_interactions SET interaction_count = interaction_count + 1, last_interaction_timestamp = :timestamp WHERE user_id = :userId AND interaction_type = :type")
    suspend fun incrementInteractionCount(userId: String, type: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM user_interactions WHERE user_id = :userId")
    suspend fun deleteUserInteractions(userId: String)
    
    @Query("DELETE FROM user_interactions WHERE user_id = :userId AND interaction_type = :type")
    suspend fun deleteUserInteractionsByType(userId: String, type: String)
    
    @Transaction
    suspend fun recordInteraction(userId: String, type: String) {
        val existing = getUserInteractionsByType(userId, type).firstOrNull()
        if (existing != null) {
            incrementInteractionCount(userId, type)
        } else {
            insertUserInteraction(
                UserInteractionEntity(
                    user_id = userId,
                    interaction_type = type,
                    interaction_count = 1,
                    last_interaction_timestamp = System.currentTimeMillis()
                )
            )
        }
    }
}
