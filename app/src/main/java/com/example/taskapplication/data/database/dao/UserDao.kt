package com.example.taskapplication.data.database.dao

import androidx.room.*
import com.example.taskapplication.data.database.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>) // For sync

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE syncStatus != 'synced'")
    suspend fun getPendingSyncUsers(): List<UserEntity> // If users can be updated locally

    // Add specific queries as needed
} 