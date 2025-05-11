package com.example.taskapplication.domain.repository

import com.example.taskapplication.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getAllUsers(): Flow<List<User>>
    suspend fun getUserById(id: String): User?
    suspend fun createUser(user: User): Result<User>
    suspend fun updateUser(user: User): Result<User>
    suspend fun deleteUser(userId: String): Result<Unit>
    suspend fun syncUsers(): Result<Unit>
    suspend fun getCurrentUser(): User?
    fun observeCurrentUser(): Flow<User?>

    /**
     * Tìm kiếm người dùng theo tên hoặc email
     * @param query Chuỗi tìm kiếm (tên hoặc email)
     * @return Danh sách người dùng phù hợp với query
     */
    suspend fun searchUsers(query: String): List<User>
}