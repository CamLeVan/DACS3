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

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: String)

    @Query("SELECT * FROM users WHERE syncStatus != 'synced'")
    suspend fun getPendingSyncUsers(): List<UserEntity> // If users can be updated locally

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?

    /**
     * Tìm kiếm người dùng theo tên hoặc email
     * @param query Chuỗi tìm kiếm (tên hoặc email)
     * @return Danh sách người dùng phù hợp với query
     */
    @Query("SELECT * FROM users WHERE name LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%'")
    suspend fun searchUsers(query: String): List<UserEntity>

    /**
     * Lấy danh sách người dùng phổ biến dựa trên số lần tương tác
     * @param limit Số lượng người dùng tối đa cần lấy
     * @return Danh sách người dùng phổ biến
     */
    @Query("SELECT * FROM users LIMIT :limit")
    suspend fun getFrequentUsers(limit: Int): List<UserEntity>

    /**
     * Lấy danh sách người dùng đã từng làm việc cùng trong các team
     * @param limit Số lượng người dùng tối đa cần lấy
     * @return Danh sách người dùng đã từng làm việc cùng
     */
    @Query("SELECT * FROM users LIMIT :limit")
    suspend fun getRecentCollaborators(limit: Int): List<UserEntity>

    // Add specific queries as needed
}