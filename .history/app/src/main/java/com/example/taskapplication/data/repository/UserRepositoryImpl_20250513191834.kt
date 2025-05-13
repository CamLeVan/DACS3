package com.example.taskapplication.data.repository

import android.util.Log
import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.api.request.UserRequest
import com.example.taskapplication.data.database.dao.UserDao
import com.example.taskapplication.data.mapper.toDomainModel
import com.example.taskapplication.data.mapper.toEntity
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.domain.model.User
import com.example.taskapplication.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.IOException
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
        return userDao.getAllUsers()
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getUserById(id: String): User? {
        val localUser = userDao.getUserById(id)

        if (localUser != null) {
            return localUser.toDomainModel()
        }

        // Nếu không tìm thấy trong local database và có kết nối mạng, thử lấy từ server
        if (connectionChecker.isNetworkAvailable()) {
            try {
                val response = apiService.getUser(id)
                if (response.isSuccessful && response.body() != null) {
                    val userResponse = response.body()!!
                    val user = userResponse.toDomainModel()
                    userDao.insertUser(user.toEntity())
                    return user
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching user from server", e)
            }
        }

        return null
    }

    override suspend fun createUser(user: User): Result<User> {
        try {
            // Lưu vào local database trước
            val userEntity = user.toEntity().copy(
                syncStatus = "pending_create",
                lastModified = System.currentTimeMillis()
            )
            userDao.insertUser(userEntity)

            // Nếu có kết nối mạng, đồng bộ lên server
            if (connectionChecker.isNetworkAvailable()) {
                try {
                    val userRequest = UserRequest(
                        name = user.name,
                        email = user.email,
                        avatar = user.avatar
                    )

                    val response = apiService.createUser(userRequest)
                    if (response.isSuccessful && response.body() != null) {
                        val userResponse = response.body()!!
                        val serverUser = userResponse.toDomainModel()

                        // Cập nhật thông tin từ server
                        val updatedUserEntity = userEntity.copy(
                            serverId = serverUser.serverId ?: "",
                            syncStatus = "synced",
                            lastModified = System.currentTimeMillis()
                        )
                        userDao.updateUser(updatedUserEntity)

                        return Result.success(updatedUserEntity.toDomainModel())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing user to server", e)
                    // Không trả về lỗi vì đã lưu thành công vào local database
                }
            }

            return Result.success(userEntity.toDomainModel())
        } catch (e: Exception) {
            Log.e(TAG, "Error creating user", e)
            return Result.failure(e)
        }
    }

    override suspend fun updateUser(user: User): Result<User> {
        try {
            // Lưu vào local database trước
            val userEntity = user.toEntity().copy(
                syncStatus = "pending_update",
                lastModified = System.currentTimeMillis()
            )
            userDao.updateUser(userEntity)

            // Nếu có kết nối mạng, đồng bộ lên server
            if (connectionChecker.isNetworkAvailable() && user.serverId != null) {
                try {
                    val userRequest = UserRequest(
                        name = user.name,
                        email = user.email,
                        avatar = user.avatar
                    )

                    val response = apiService.updateUser(user.serverId, userRequest)
                    if (response.isSuccessful && response.body() != null) {
                        // Cập nhật trạng thái đồng bộ
                        val updatedUserEntity = userEntity.copy(
                            syncStatus = "synced",
                            lastModified = System.currentTimeMillis()
                        )
                        userDao.updateUser(updatedUserEntity)

                        return Result.success(updatedUserEntity.toDomainModel())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing user update to server", e)
                    // Không trả về lỗi vì đã lưu thành công vào local database
                }
            }

            return Result.success(userEntity.toDomainModel())
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user", e)
            return Result.failure(e)
        }
    }

    override suspend fun deleteUser(userId: String): Result<Unit> {
        try {
            val user = userDao.getUserById(userId)
            if (user != null) {
                // Nếu user đã được đồng bộ với server, đánh dấu để xóa sau
                if (user.serverId != null) {
                    val updatedUser = user.copy(
                        syncStatus = "pending_delete",
                        lastModified = System.currentTimeMillis()
                    )
                    userDao.updateUser(updatedUser)

                    // Nếu có kết nối mạng, đồng bộ lên server
                    if (connectionChecker.isNetworkAvailable()) {
                        try {
                            val response = apiService.deleteUser(user.serverId)
                            if (response.isSuccessful) {
                                userDao.deleteUserById(user.id)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error syncing user deletion to server", e)
                            // Không trả về lỗi vì đã xử lý thành công trong local database
                        }
                    }
                } else {
                    // Nếu user chưa được đồng bộ với server, xóa luôn
                    userDao.deleteUserById(user.id)
                }
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user", e)
            return Result.failure(e)
        }
    }

    override suspend fun syncUsers(): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        try {
            // Lấy danh sách user từ server
            val response = apiService.getUsers()
            if (response.isSuccessful && response.body() != null) {
                val userResponses = response.body()!!
                val users = userResponses.map { it.toDomainModel() }

                // Lưu vào local database
                val userEntities = users.map { it.toEntity().copy(syncStatus = "synced") }
                userDao.insertUsers(userEntities)

                return Result.success(Unit)
            } else {
                return Result.failure(IOException("Failed to sync users: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing users", e)
            return Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): User? {
        val userId = dataStoreManager.getCurrentUserId()
        return if (userId != null) {
            val userEntity = userDao.getUserById(userId)
            userEntity?.toDomainModel()
        } else {
            null
        }
    }

    override fun observeCurrentUser(): Flow<User?> {
        return dataStoreManager.userId.map { userId ->
            if (userId != null) {
                userDao.getUserById(userId)?.toDomainModel()
            } else {
                null
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun searchUsers(query: String, limit: Int): List<User> {
        Log.e(TAG, "🚨 SEARCH USERS CALLED: query='$query', limit=$limit")

        // Yêu cầu ít nhất 2 ký tự để tìm kiếm
        if (query.length < 2) {
            Log.e(TAG, "🚨 Query too short (< 2 chars), returning empty list")
            return emptyList()
        }

        try {
            // Tìm kiếm trong local database trước
            Log.d(TAG, "🔍 Searching in local database with query: '$query'")
            val localResultsEntities = userDao.searchUsers(query, limit)
            Log.d(TAG, "📊 Local database raw results: ${localResultsEntities.size}")

            // Log chi tiết từng kết quả từ database
            localResultsEntities.forEachIndexed { index, entity ->
                Log.d(TAG, "📝 Local DB result #${index + 1}: id=${entity.id}, name='${entity.name}', email='${entity.email}'")
            }

            val localResults = localResultsEntities.map { it.toDomainModel() }
            Log.d(TAG, "📊 Local results after mapping: ${localResults.size}")

            // Nếu có kết nối mạng, thử tìm kiếm trên server
            if (connectionChecker.isNetworkAvailable()) {
                Log.e(TAG, "🚨 Network available, trying server search")
                try {
                    Log.e(TAG, "🚨 Calling API: searchUsers('$query')")

                    // Kiểm tra URL endpoint
                    val apiUrl = "users/search?query=$query"
                    Log.e(TAG, "🚨 API URL: $apiUrl")

                    // Kiểm tra token xác thực
                    val token = runBlocking { dataStoreManager.authToken.first() }
                    Log.e(TAG, "🚨 Auth token available: ${!token.isNullOrEmpty()}")

                    val response = apiService.searchUsers(query)
                    Log.e(TAG, "🚨 API response code: ${response.code()}")

                    if (response.isSuccessful) {
                        Log.e(TAG, "🚨 API call successful")
                        val responseBody = response.body()
                        if (responseBody != null) {
                            // Xử lý cả hai trường hợp: API trả về danh sách users hoặc một user duy nhất
                            val usersList = if (responseBody.user != null) {
                                // Nếu API trả về một user duy nhất trong trường "user"
                                Log.e(TAG, "🚨 Server returned a single user in 'user' field")
                                listOf(responseBody.user)
                            } else {
                                // Nếu API trả về danh sách users trong trường "users"
                                responseBody.users
                            }

                            Log.e(TAG, "🚨 Server results count: ${usersList.size}")
                            Log.e(TAG, "🚨 Server response total: ${responseBody.total}")
                            Log.e(TAG, "🚨 Server response message: ${responseBody.message}")
                            Log.e(TAG, "🚨 Server response status: ${responseBody.status}")

                            // Log chi tiết từng kết quả từ server
                            usersList.forEachIndexed { index, userResponse ->
                                Log.e(TAG, "🚨 Server result #${index + 1}: id=${userResponse.id}, name='${userResponse.name}', email='${userResponse.email}'")
                            }

                            val serverResults = usersList.map { it.toDomainModel() }
                            Log.e(TAG, "🚨 Server results after mapping: ${serverResults.size}")

                            // Kết hợp kết quả từ local và server, loại bỏ trùng lặp và giới hạn số lượng
                            val combinedResults = (localResults + serverResults)
                                .distinctBy { it.id }
                                .take(limit)
                            Log.e(TAG, "🚨 Combined results after deduplication: ${combinedResults.size}")
                            return combinedResults
                        } else {
                            Log.e(TAG, "🚨 API response body is null")
                        }
                    } else {
                        Log.e(TAG, "🚨 API call failed with code: ${response.code()}")
                        try {
                            val errorBody = response.errorBody()?.string()
                            Log.e(TAG, "🚨 Error body: $errorBody")
                        } catch (e: Exception) {
                            Log.e(TAG, "🚨 Could not read error body", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "🚨 Error searching users from server", e)
                    Log.e(TAG, "🚨 Exception details: ${e.message}", e)
                    Log.e(TAG, "🚨 Exception stack trace: ${e.stackTraceToString()}")
                    // Nếu có lỗi khi tìm kiếm từ server, vẫn trả về kết quả từ local
                }
            } else {
                Log.e(TAG, "🚨 No network connection, using only local results")
            }

            Log.e(TAG, "🚨 Returning local results: ${localResults.size}")
            return localResults
        } catch (e: Exception) {
            Log.e(TAG, "🚨 Error searching users", e)
            Log.e(TAG, "🚨 Exception details: ${e.message}")
            Log.e(TAG, "🚨 Exception stack trace: ${e.stackTraceToString()}")
            return emptyList()
        }
    }

    override suspend fun getFrequentUsers(limit: Int): List<User> {
        try {
            // Lấy danh sách người dùng phổ biến
            return userDao.getFrequentUsers(limit).map { it.toDomainModel() }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting frequent users", e)
            return emptyList()
        }
    }

    override suspend fun getRecentCollaborators(limit: Int): List<User> {
        try {
            // Lấy danh sách người dùng đã từng làm việc cùng
            val collaborators = userDao.getRecentCollaborators(limit).map { it.toDomainModel() }

            // Nếu không đủ số lượng, bổ sung bằng người dùng phổ biến
            if (collaborators.size < limit) {
                val frequentUsers = getFrequentUsers(limit - collaborators.size)
                    .filter { user -> !collaborators.any { it.id == user.id } }

                return collaborators + frequentUsers
            }

            return collaborators
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recent collaborators", e)
            return emptyList()
        }
    }
}
