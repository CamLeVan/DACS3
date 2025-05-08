package com.example.taskapplication.data.repository

import android.util.Log
import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.database.dao.TeamTaskDao
import com.example.taskapplication.data.mapper.toDomainModel
import com.example.taskapplication.data.mapper.toEntity
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.domain.model.TeamTask
import com.example.taskapplication.domain.repository.TeamTaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeamTaskRepositoryImpl @Inject constructor(
    private val teamTaskDao: TeamTaskDao,
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager,
    private val connectionChecker: ConnectionChecker
) : TeamTaskRepository {

    private val TAG = "TeamTaskRepository"

    override fun getAllTeamTasks(): Flow<List<TeamTask>> {
        return teamTaskDao.getAllTeamTasks()
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getTasksByTeam(teamId: String): Flow<List<TeamTask>> {
        return teamTaskDao.getTasksByTeam(teamId)
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getTasksAssignedToUser(userId: String): Flow<List<TeamTask>> {
        return teamTaskDao.getTasksAssignedToUser(userId)
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getTaskById(id: String): TeamTask? {
        return teamTaskDao.getTaskById(id)?.toDomainModel()
    }

    override suspend fun createTask(task: TeamTask): Result<TeamTask> {
        try {
            // Tạo ID mới nếu chưa có
            val taskWithId = if (task.id.isBlank()) {
                task.copy(id = UUID.randomUUID().toString())
            } else {
                task
            }

            // Lưu vào local database trước
            val taskEntity = taskWithId.toEntity().copy(
                syncStatus = "pending_create",
                lastModified = System.currentTimeMillis()
            )
            teamTaskDao.insertTask(taskEntity)

            // Nếu có kết nối mạng, đồng bộ lên server
            if (connectionChecker.isNetworkAvailable()) {
                try {
                    // Triển khai đồng bộ với server ở đây
                    // Hiện tại chỉ trả về thành công vì chúng ta đã lưu vào local database
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing task to server", e)
                    // Không trả về lỗi vì đã lưu thành công vào local database
                }
            }

            return Result.success(taskEntity.toDomainModel())
        } catch (e: Exception) {
            Log.e(TAG, "Error creating task", e)
            return Result.failure(e)
        }
    }

    override suspend fun updateTask(task: TeamTask): Result<TeamTask> {
        try {
            // Lưu vào local database trước
            val taskEntity = task.toEntity().copy(
                syncStatus = "pending_update",
                lastModified = System.currentTimeMillis()
            )
            teamTaskDao.updateTask(taskEntity)

            // Nếu có kết nối mạng, đồng bộ lên server
            if (connectionChecker.isNetworkAvailable()) {
                try {
                    // Triển khai đồng bộ với server ở đây
                    // Hiện tại chỉ trả về thành công vì chúng ta đã lưu vào local database
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing task update to server", e)
                    // Không trả về lỗi vì đã lưu thành công vào local database
                }
            }

            return Result.success(taskEntity.toDomainModel())
        } catch (e: Exception) {
            Log.e(TAG, "Error updating task", e)
            return Result.failure(e)
        }
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> {
        try {
            val task = teamTaskDao.getTaskById(taskId)
            if (task != null) {
                if (task.serverId != null) {
                    // Nếu task đã được đồng bộ với server, đánh dấu để xóa sau
                    val updatedTask = task.copy(
                        syncStatus = "pending_delete",
                        lastModified = System.currentTimeMillis()
                    )
                    teamTaskDao.updateTask(updatedTask)
                } else {
                    // Nếu task chưa được đồng bộ với server, xóa luôn
                    teamTaskDao.deleteTask(task)
                }

                // Nếu có kết nối mạng, đồng bộ lên server
                if (connectionChecker.isNetworkAvailable() && task.serverId != null) {
                    try {
                        // Triển khai xóa trên server ở đây
                        // Hiện tại chỉ trả về thành công vì chúng ta đã xử lý trong local database
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing task deletion to server", e)
                        // Không trả về lỗi vì đã xử lý thành công trong local database
                    }
                }
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting task", e)
            return Result.failure(e)
        }
    }

    override suspend fun assignTask(taskId: String, userId: String?): Result<TeamTask> {
        try {
            val task = teamTaskDao.getTaskById(taskId)
            if (task != null) {
                val updatedTask = task.copy(
                    assignedUserId = userId,
                    syncStatus = "pending_update",
                    lastModified = System.currentTimeMillis()
                )
                teamTaskDao.updateTask(updatedTask)

                // Nếu có kết nối mạng, đồng bộ lên server
                if (connectionChecker.isNetworkAvailable() && task.serverId != null) {
                    try {
                        // Triển khai đồng bộ với server ở đây
                        // Hiện tại chỉ trả về thành công vì chúng ta đã lưu vào local database
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing task assignment to server", e)
                        // Không trả về lỗi vì đã lưu thành công vào local database
                    }
                }

                return Result.success(updatedTask.toDomainModel())
            } else {
                return Result.failure(IOException("Task not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error assigning task", e)
            return Result.failure(e)
        }
    }

    override suspend fun syncTasks(): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        try {
            // Triển khai đồng bộ với server ở đây
            // 1. Đẩy các thay đổi local lên server
            // 2. Lấy các thay đổi từ server về

            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing tasks", e)
            return Result.failure(e)
        }
    }

    override suspend fun syncTasksByTeam(teamId: String): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        try {
            // Triển khai đồng bộ với server ở đây
            // 1. Đẩy các thay đổi local lên server
            // 2. Lấy các thay đổi từ server về

            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing team tasks", e)
            return Result.failure(e)
        }
    }
}
