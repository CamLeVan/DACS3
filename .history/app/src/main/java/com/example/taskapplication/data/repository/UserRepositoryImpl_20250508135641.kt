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
import kotlinx.coroutines.flow.catch
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
    private val connectionChecker: ConnectionChecker,
    private val networkUtils: NetworkUtils
) : UserRepository {

    private val TAG = "UserRepository"

    override fun getUsers(): Flow<List<User>> {
        return userDao.getAllUsers()
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                // Log error and emit empty list
                emit(emptyList())
            }
    }

    override fun getUserById(userId: String): Flow<User?> {
        return userDao.getUserById(userId)
            .map { entity -> entity?.toDomainModel() }
            .catch { e ->
                // Log error and emit null
                emit(null)
            }
    }

    override suspend fun createUser(user: User): Result<User> {
        return try {
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.createUser(user.toEntity().toApiRequest())
                if (response.isSuccessful) {
                    response.body()?.let { userResponse ->
                        val entity = userResponse.toEntity()
                        userDao.insertUser(entity)
                        Result.success(entity.toDomainModel())
                    } ?: Result.failure(IOException("Empty response from server"))
                } else {
                    // If server fails, save locally with pending status
                    val entity = user.toEntity().copy(syncStatus = "pending_create")
                    userDao.insertUser(entity)
                    Result.success(entity.toDomainModel())
                }
            } else {
                // Save locally with pending status
                val entity = user.toEntity().copy(syncStatus = "pending_create")
                userDao.insertUser(entity)
                Result.success(entity.toDomainModel())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUser(user: User): Result<User> {
        return try {
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.updateUser(user.toEntity().toApiRequest())
                if (response.isSuccessful) {
                    response.body()?.let { userResponse ->
                        val entity = userResponse.toEntity()
                        userDao.updateUser(entity)
                        Result.success(entity.toDomainModel())
                    } ?: Result.failure(IOException("Empty response from server"))
                } else {
                    // If server fails, update locally with pending status
                    val entity = user.toEntity().copy(syncStatus = "pending_update")
                    userDao.updateUser(entity)
                    Result.success(entity.toDomainModel())
                }
            } else {
                // Update locally with pending status
                val entity = user.toEntity().copy(syncStatus = "pending_update")
                userDao.updateUser(entity)
                Result.success(entity.toDomainModel())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            if (networkUtils.isNetworkAvailable()) {
                val response = apiService.deleteUser(userId)
                if (response.isSuccessful) {
                    userDao.deleteUser(userId)
                    Result.success(Unit)
                } else {
                    // If server fails, mark for deletion locally
                    userDao.markUserForDeletion(userId, System.currentTimeMillis())
                    Result.success(Unit)
                }
            } else {
                // Mark for deletion locally
                userDao.markUserForDeletion(userId, System.currentTimeMillis())
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncUsers(): Result<Unit> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                return Result.failure(IOException("No network connection"))
            }

            // Get pending users
            val pendingUsers = userDao.getPendingSyncUsers()

            // Sync each pending user
            for (user in pendingUsers) {
                when (user.syncStatus) {
                    "pending_create" -> {
                        val response = apiService.createUser(user.toApiRequest())
                        if (response.isSuccessful) {
                            response.body()?.let { userResponse ->
                                val entity = userResponse.toEntity()
                                userDao.updateUser(entity)
                            }
                        }
                    }
                    "pending_update" -> {
                        val response = apiService.updateUser(user.toApiRequest())
                        if (response.isSuccessful) {
                            response.body()?.let { userResponse ->
                                val entity = userResponse.toEntity()
                                userDao.updateUser(entity)
                            }
                        }
                    }
                    "pending_delete" -> {
                        val response = apiService.deleteUser(user.id)
                        if (response.isSuccessful) {
                            userDao.deleteUser(user.id)
                        }
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
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