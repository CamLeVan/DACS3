package com.example.taskapplication.data.repository

import android.util.Log
import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.database.dao.PersonalTaskDao
import com.example.taskapplication.data.mapper.toDomainModel
import com.example.taskapplication.data.mapper.toEntity
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.domain.model.PersonalTask
import com.example.taskapplication.domain.repository.PersonalTaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonalTaskRepositoryImpl @Inject constructor(
    private val personalTaskDao: PersonalTaskDao,
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager,
    private val connectionChecker: ConnectionChecker
) : PersonalTaskRepository {

    private val TAG = "PersonalTaskRepository"

    override fun getAllTasks(): Flow<List<PersonalTask>> {
        return personalTaskDao.getAllTasks()
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getTask(id: String): PersonalTask? {
        return personalTaskDao.getTaskById(id)?.toDomainModel()
    }

    override suspend fun createTask(task: PersonalTask): Result<PersonalTask> {
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
            personalTaskDao.insertTask(taskEntity)

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

    override suspend fun updateTask(task: PersonalTask): Result<PersonalTask> {
        try {
            // Lưu vào local database trước
            val taskEntity = task.toEntity().copy(
                syncStatus = "pending_update",
                lastModified = System.currentTimeMillis()
            )
            personalTaskDao.updateTask(taskEntity)

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
            val task = personalTaskDao.getTaskById(taskId)
            if (task != null) {
                if (task.serverId != null) {
                    // Nếu task đã được đồng bộ với server, đánh dấu để xóa sau
                    personalTaskDao.markTaskForDeletion(taskId, System.currentTimeMillis())
                } else {
                    // Nếu task chưa được đồng bộ với server, xóa luôn
                    personalTaskDao.deleteLocalOnlyTask(taskId)
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
}
