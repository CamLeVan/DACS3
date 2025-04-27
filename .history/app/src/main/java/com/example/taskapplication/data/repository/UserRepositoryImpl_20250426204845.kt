package com.example.taskapplication.data.repository

import android.util.Log
import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.api.request.LoginRequest
import com.example.taskapplication.data.api.request.RegisterRequest
import com.example.taskapplication.data.database.dao.UserDao
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
        val userEntity = user.copy(
            id = UUID.randomUUID().toString(),
            syncStatus = "pending_create",
            lastModified = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        ).toEntity()

        userDao.insertUser(userEntity)
        return Result.success(userEntity.toDomainModel())
    }

    override suspend fun updateUser(user: User): Result<User> {
        val existingUser = userDao.getUserById(user.id)
            ?: return Result.failure(NoSuchElementException("User not found"))

        val updatedEntity = user.copy(
            syncStatus = if (existingUser.serverId != null) "pending_update" else "pending_create",
            lastModified = System.currentTimeMillis(),
            serverId = existingUser.serverId,
            createdAt = existingUser.createdAt
        ).toEntity()

        userDao.updateUser(updatedEntity)
        return Result.success(updatedEntity.toDomainModel())
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
            // This would be implemented to sync user data with the server
            // For simplicity, we'll leave this as a placeholder
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error during user sync", e)
            return Result.failure(e)
        }
    }

    override suspend fun login(email: String, password: String): Result<User> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        try {
            val deviceId = getOrCreateDeviceId()
            val response = apiService.login(LoginRequest(email, password, deviceId))

            if (!response.isSuccessful || response.body() == null) {
                return Result.failure(IOException("Login failed: ${response.message()}"))
            }

            val authResponse = response.body()!!
            
            // Save auth token
            dataStoreManager.saveAuthToken(authResponse.token)
            authResponse.refreshToken?.let { dataStoreManager.saveRefreshToken(it) }
            dataStoreManager.saveTokenExpiresAt(authResponse.expiresAt)
            
            // Save user info
            val user = authResponse.user
            dataStoreManager.saveUserId(user.id.toString())
            dataStoreManager.saveUserInfo(user.name, user.email)
            
            // Save to local database
            val userEntity = User(
                id = user.id.toString(),
                name = user.name,
                email = user.email,
                avatarUrl = user.avatarUrl,
                serverId = user.id,
                syncStatus = "synced",
                lastModified = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            ).toEntity()
            
            userDao.insertUser(userEntity)
            
            return Result.success(userEntity.toDomainModel())
        } catch (e: Exception) {
            Log.e(TAG, "Error during login", e)
            return Result.failure(e)
        }
    }

    override suspend fun register(name: String, email: String, password: String): Result<User> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        try {
            val deviceId = getOrCreateDeviceId()
            val request = RegisterRequest(
                name = name,
                email = email,
                password = password,
                passwordConfirmation = password,
                deviceId = deviceId
            )
            val response = apiService.register(request)

            if (!response.isSuccessful || response.body() == null) {
                return Result.failure(IOException("Registration failed: ${response.message()}"))
            }

            val authResponse = response.body()!!
            
            // Save auth token
            dataStoreManager.saveAuthToken(authResponse.token)
            authResponse.refreshToken?.let { dataStoreManager.saveRefreshToken(it) }
            dataStoreManager.saveTokenExpiresAt(authResponse.expiresAt)
            
            // Save user info
            val user = authResponse.user
            dataStoreManager.saveUserId(user.id.toString())
            dataStoreManager.saveUserInfo(user.name, user.email)
            
            // Save to local database
            val userEntity = User(
                id = user.id.toString(),
                name = user.name,
                email = user.email,
                avatarUrl = user.avatarUrl,
                serverId = user.id,
                syncStatus = "synced",
                lastModified = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            ).toEntity()
            
            userDao.insertUser(userEntity)
            
            return Result.success(userEntity.toDomainModel())
        } catch (e: Exception) {
            Log.e(TAG, "Error during registration", e)
            return Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        try {
            // Clear auth data
            dataStoreManager.clearAll()
            
            // We typically don't clear the database on logout for offline capability
            // but you might want to clear sensitive data
            
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error during logout", e)
            return Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): User? {
        // Get current user ID from DataStore
        val userId = dataStoreManager.userId.first() ?: return null
        
        // Fetch user from local database
        return userDao.getUserById(userId)?.toDomainModel()
    }

    override fun observeCurrentUser(): Flow<User?> {
        return flow {
            val userId = dataStoreManager.userId.first()
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
        var deviceId = dataStoreManager.deviceId.first()
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            dataStoreManager.saveDeviceId(deviceId)
        }
        return deviceId
    }
} 