package com.example.taskapplication.data.repository

import android.util.Log
import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.database.dao.UserDao
import com.example.taskapplication.data.mapper.toApiRequest
import com.example.taskapplication.data.mapper.toDomainModel
import com.example.taskapplication.data.mapper.toEntity
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.domain.model.User
import com.example.taskapplication.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager,
    private val connectionChecker: ConnectionChecker
) : UserRepository {

    private val TAG = "UserRepository"

    override fun getAllUsers(): Flow<List<User>> {
        return userDao.getAllUsers().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getUserById(id: String): User? {
        return userDao.getUserById(id)?.toDomainModel()
    }

    override suspend fun createUser(user: User): Result<User> {
        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        val userWithId = user.copy(
            id = UUID.randomUUID().toString(),
            syncStatus = "pending_create",
            lastModified = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )

        userDao.insertUser(userWithId.toEntity())

        // Nếu có kết nối, thử đồng bộ ngay
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncUsers()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after creating user", e)
            }
        }

        return Result.success(userWithId)
    }

    override suspend fun updateUser(user: User): Result<User> {
        val currentUserId = dataStoreManager.getCurrentUserId() ?:
            return Result.failure(IllegalStateException("No current user found"))

        val existingUser = userDao.getUserById(user.id)
            ?: return Result.failure(NoSuchElementException("User not found"))

        // Chỉ cho phép người dùng sửa thông tin của chính họ
        if (user.id != currentUserId) {
            return Result.failure(IllegalStateException("You can only update your own profile"))
        }

        val updatedUser = user.copy(
            syncStatus = if (existingUser.serverId != null) "pending_update" else "pending_create",
            lastModified = System.currentTimeMillis(),
            serverId = existingUser.serverId,
            createdAt = existingUser.createdAt
        )

        userDao.updateUser(updatedUser.toEntity())

        // Nếu có kết nối, thử đồng bộ ngay
        if (connectionChecker.isNetworkAvailable()) {
            try {
                syncUsers()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing after updating user", e)
            }
        }

        return Result.success(updatedUser)
    }

    override suspend fun deleteUser(userId: String): Result<Unit> {
        // This is typically not implemented for users, as deleting a user is usually a server-side operation
        return Result.failure(UnsupportedOperationException("Deleting users is not supported"))
    }

    override suspend fun syncUsers(): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        try {
            val pendingUsers = userDao.getPendingSyncUsers()

            // Group by sync status
            val usersToCreate = pendingUsers.filter { it.syncStatus == "pending_create" }
            val usersToUpdate = pendingUsers.filter { it.syncStatus == "pending_update" }

            // Process creates
            for (user in usersToCreate) {
                try {
                    val response = apiService.createUser(user.toApiRequest())
                    if (response.isSuccessful && response.body() != null) {
                        val serverUser = response.body()!!
                        userDao.updateUserServerId(user.id, serverUser.id.toString())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating user on server", e)
                }
            }

            // Process updates
            for (user in usersToUpdate) {
                try {
                    if (user.serverId == null) continue

                    val response = apiService.updateUser(
                        userId = user.serverId.toLong(),
                        user = user.toApiRequest()
                    )

                    if (response.isSuccessful) {
                        userDao.markUserAsSynced(user.id)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating user on server", e)
                }
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing users", e)
            return Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): User? {
        // Get current user ID from DataStore
        val userId = dataStoreManager.getCurrentUserId() ?: return null

        // Fetch user from local database
        return userDao.getUserById(userId)?.toDomainModel()
    }

    override fun observeCurrentUser(): Flow<User?> {
        return flow {
            val userId = dataStoreManager.getCurrentUserId()
            if (userId != null) {
                userDao.getUserById(userId)?.let {
                    emit(it.toDomainModel())
                } ?: emit(null)
            } else {
                emit(null)
            }
        }
    }

    private suspend fun getOrCreateDeviceId(): String {
        var deviceId = dataStoreManager.getDeviceId()
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            dataStoreManager.saveDeviceId(deviceId)
        }
        return deviceId
    }
}