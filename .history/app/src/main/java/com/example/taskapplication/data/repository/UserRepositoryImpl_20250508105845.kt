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
    private val api: ApiService,
    private val dataStoreManager: DataStoreManager,
    private val connectionChecker: ConnectionChecker,
    private val networkUtils: NetworkUtils
) : UserRepository {

    private val TAG = "UserRepository"

    override fun getUsers(): Flow<List<User>> = flow {
        userDao.getAllUsers().collect { entities ->
            emit(entities.map { it.toDomainModel() })
        }
    }

    override fun getUsersSync(): Flow<List<User>> = flow {
        userDao.getAllUsersSync().collect { entities ->
            emit(entities.map { it.toDomainModel() })
        }
    }

    override suspend fun getUserById(id: String): User? {
        return userDao.getUserById(id)?.toDomainModel()
    }

    override suspend fun getUserByIdSync(id: String): User? {
        return userDao.getUserByIdSync(id)?.toDomainModel()
    }

    override suspend fun getUserByServerId(serverId: String): User? {
        return userDao.getUserByServerId(serverId)?.toDomainModel()
    }

    override suspend fun getUserByServerIdSync(serverId: String): User? {
        return userDao.getUserByServerIdSync(serverId)?.toDomainModel()
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
            Result.failure(e)
        }
    }

    override suspend fun deleteUser(user: User): Result<Unit> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                val entity = user.toEntity()
                entity.syncStatus = "pending"
                userDao.updateUser(entity)
                Result.success(Unit)
            } else {
                api.deleteUser(user.id)
                userDao.deleteUser(user.toEntity())
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncUsers(): Result<Unit> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                return Result.failure(Exception("No network connection"))
            }

            val pendingUsers = userDao.getPendingUsersSync()
            for (user in pendingUsers) {
                when (user.syncStatus) {
                    "pending" -> {
                        val response = api.createUser(user.toApiRequest())
                        val entity = response.toEntity()
                        entity.syncStatus = "synced"
                        userDao.updateUser(entity)
                    }
                }
            }

            val serverUsers = api.getUsers()
            for (user in serverUsers) {
                val entity = user.toEntity()
                entity.syncStatus = "synced"
                userDao.insertUser(entity)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncUsersByTeam(teamId: String): Result<Unit> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                return Result.failure(Exception("No network connection"))
            }

            val pendingUsers = userDao.getPendingUsersByTeamSync(teamId)
            for (user in pendingUsers) {
                when (user.syncStatus) {
                    "pending" -> {
                        val response = api.createUser(user.toApiRequest())
                        val entity = response.toEntity()
                        entity.syncStatus = "synced"
                        userDao.updateUser(entity)
                    }
                }
            }

            val serverUsers = api.getUsersByTeam(teamId)
            for (user in serverUsers) {
                val entity = user.toEntity()
                entity.syncStatus = "synced"
                userDao.insertUser(entity)
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