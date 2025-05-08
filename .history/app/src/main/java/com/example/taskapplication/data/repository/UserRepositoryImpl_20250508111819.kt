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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
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

    override fun getUsers(): Flow<List<User>> = flow {
        val users = userDao.getAllUsers().map { it.toDomainModel() }
        emit(users)
    }

    override fun getUsersByTeam(teamId: String): Flow<List<User>> = flow {
        val users = userDao.getUsersByTeam(teamId.toLong()).map { it.toDomainModel() }
        emit(users)
    }

    override suspend fun getUsersSync(): List<User> = withContext(Dispatchers.IO) {
        userDao.getAllUsersSync().map { it.toDomainModel() }
    }

    override suspend fun getUserById(id: String): User? = withContext(Dispatchers.IO) {
        userDao.getUserById(id.toLong())?.toDomainModel()
    }

    override suspend fun getUserByIdSync(id: String): User? = withContext(Dispatchers.IO) {
        userDao.getUserByIdSync(id.toLong())?.toDomainModel()
    }

    override suspend fun getUserByServerId(serverId: String): User? = withContext(Dispatchers.IO) {
        userDao.getUserByServerId(serverId)?.toDomainModel()
    }

    override suspend fun getUserByServerIdSync(serverId: String): User? = withContext(Dispatchers.IO) {
        userDao.getUserByServerIdSync(serverId)?.toDomainModel()
    }

    override suspend fun createUser(user: User): Result<User> = withContext(Dispatchers.IO) {
        try {
            val entity = user.toEntity()
            entity.syncStatus = if (networkUtils.isNetworkAvailable()) "synced" else "pending"
            val id = userDao.insertUser(entity)
            val createdUser = userDao.getUserById(id)?.toDomainModel()
                ?: return@withContext Result.failure(Exception("Failed to create user"))
            
            if (networkUtils.isNetworkAvailable()) {
                val response = api.createUser(entity.toApiRequest())
                val updatedEntity = response.toEntity(entity)
                userDao.updateUser(updatedEntity)
                Result.success(updatedEntity.toDomainModel())
            } else {
                Result.success(createdUser)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUser(user: User): Result<User> = withContext(Dispatchers.IO) {
        try {
            val entity = user.toEntity()
            entity.syncStatus = if (networkUtils.isNetworkAvailable()) "synced" else "pending"
            userDao.updateUser(entity)
            val updatedUser = userDao.getUserById(entity.id)?.toDomainModel()
                ?: return@withContext Result.failure(Exception("Failed to update user"))
            
            if (networkUtils.isNetworkAvailable()) {
                val response = api.updateUser(entity.id, entity.toApiRequest())
                val updatedEntity = response.toEntity(entity)
                userDao.updateUser(updatedEntity)
                Result.success(updatedEntity.toDomainModel())
            } else {
                Result.success(updatedUser)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteUser(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = userDao.getUserById(userId.toLong())
                ?: return@withContext Result.failure(Exception("User not found"))

            if (networkUtils.isNetworkAvailable()) {
                api.deleteUser(user.id)
                userDao.deleteUser(user)
                Result.success(Unit)
            } else {
                user.syncStatus = "pending"
                userDao.updateUser(user)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncUsers(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!networkUtils.isNetworkAvailable()) {
                return@withContext Result.failure(Exception("No network connection"))
            }

            val pendingUsers = userDao.getPendingUsersSync()
            for (user in pendingUsers) {
                when (user.syncStatus) {
                    "pending" -> {
                        val response = api.createUser(user.toApiRequest())
                        val updatedEntity = response.toEntity(user)
                        userDao.updateUser(updatedEntity)
                    }
                    "deleted" -> {
                        api.deleteUser(user.id)
                        userDao.deleteUser(user)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncUsersByTeam(teamId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!networkUtils.isNetworkAvailable()) {
                return@withContext Result.failure(Exception("No network connection"))
            }

            val pendingUsers = userDao.getPendingUsersByTeamSync(teamId.toLong())
            for (user in pendingUsers) {
                when (user.syncStatus) {
                    "pending" -> {
                        val response = api.createUser(user.toApiRequest())
                        val updatedEntity = response.toEntity(user)
                        userDao.updateUser(updatedEntity)
                    }
                    "deleted" -> {
                        api.deleteUser(user.id)
                        userDao.deleteUser(user)
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