package com.example.taskapplication.data.repository

import android.util.Log
import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.database.dao.UserDao
import com.example.taskapplication.data.entity.UserEntity
import com.example.taskapplication.data.mapper.toApiRequest
import com.example.taskapplication.data.mapper.toDomainModel
import com.example.taskapplication.data.mapper.toEntity
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.domain.model.User
import com.example.taskapplication.domain.repository.UserRepository
import com.example.taskapplication.util.NetworkUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val api: ApiService,
    private val dataStoreManager: DataStoreManager,
    private val connectionChecker: ConnectionChecker,
    private val networkUtils: NetworkUtils
) : UserRepository {

    private val TAG = "UserRepository"

    override fun getAllUsers(): Flow<List<User>> {
        return userDao.getAllUsers().map { users ->
            users.map { it.toDomainModel() }
        }
    }

    override suspend fun getUserById(id: String): User? {
        return userDao.getUserById(id)?.toDomainModel()
    }

    override suspend fun createUser(user: User): Result<User> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                val entity = user.toEntity()
                entity.syncStatus = "pending"
                userDao.insertUser(entity)
                Result.success(entity.toDomainModel())
            } else {
                val response = api.createUser(user.toApiRequest())
                val entity = response.toEntity()
                entity.syncStatus = "synced"
                userDao.insertUser(entity)
                Result.success(entity.toDomainModel())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating user", e)
            Result.failure(e)
        }
    }

    override suspend fun updateUser(user: User): Result<User> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                val entity = user.toEntity()
                entity.syncStatus = "pending"
                userDao.updateUser(entity)
                Result.success(entity.toDomainModel())
            } else {
                val response = api.updateUser(user.id, user.toApiRequest())
                val entity = response.toEntity()
                entity.syncStatus = "synced"
                userDao.updateUser(entity)
                Result.success(entity.toDomainModel())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                val user = userDao.getUserById(userId)
                if (user != null) {
                    user.syncStatus = "pending"
                    userDao.updateUser(user)
                }
                Result.success(Unit)
            } else {
                api.deleteUser(userId)
                userDao.deleteUser(userId)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user", e)
            Result.failure(e)
        }
    }

    override suspend fun syncUsers(): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        return try {
            // Sync pending users
            val pendingUsers = userDao.getPendingUsersSync()
            for (user in pendingUsers) {
                when (user.syncStatus) {
                    "pending_create" -> {
                        val response = api.createUser(user.toApiRequest())
                        if (response.isSuccessful) {
                            val serverId = response.body()?.id.toString()
                            userDao.updateUserServerId(user.id, serverId)
                            userDao.markUserAsSynced(user.id)
                        }
                    }
                    "pending_update" -> {
                        val response = api.updateUser(user.serverId!!, user.toApiRequest())
                        if (response.isSuccessful) {
                            userDao.markUserAsSynced(user.id)
                        }
                    }
                    "pending_delete" -> {
                        val response = api.deleteUser(user.serverId!!)
                        if (response.isSuccessful) {
                            userDao.deleteUser(user.id)
                        }
                    }
                }
            }

            // Sync server users
            val response = api.getUsers()
            if (response.isSuccessful) {
                val serverUsers = response.body() ?: emptyList()
                for (serverUser in serverUsers) {
                    val existingUser = userDao.getUserByServerId(serverUser.id.toString())
                    if (existingUser == null) {
                        userDao.insertUser(serverUser.toEntity())
                    } else {
                        userDao.updateUser(serverUser.toEntity(existingUser))
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing users", e)
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): User? {
        // Get current user ID from DataStore
        val userId = dataStoreManager.getCurrentUserId() ?: return null

        // Fetch user from local database
        return userDao.getUserById(userId)?.toDomainModel()
    }

    override fun observeCurrentUser(): Flow<User?> {
        return dataStoreManager.observeCurrentUserId().map { userId ->
            userId?.let { userDao.getUserById(it)?.toDomainModel() }
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