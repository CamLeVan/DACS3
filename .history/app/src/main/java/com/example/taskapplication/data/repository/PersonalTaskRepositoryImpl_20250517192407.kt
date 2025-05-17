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

            // Lưu vào local database trước với trạng thái pending_create
            val taskEntity = taskWithId.toEntity().copy(
                syncStatus = "pending_create",
                lastModified = System.currentTimeMillis()
            )
            personalTaskDao.insertTask(taskEntity)

            // Nếu có kết nối mạng, đồng bộ lên server
            if (connectionChecker.isNetworkAvailable()) {
                try {
                    // Gọi API để tạo task trên server
                    val request = taskWithId.toApiRequest()
                    val response = apiService.createPersonalTask(request)

                    if (response.isSuccessful && response.body() != null) {
                        // Cập nhật local database với thông tin từ server
                        val serverTask = response.body()!!
                        val updatedEntity = serverTask.toEntity(taskEntity).copy(
                            syncStatus = "synced",
                            lastModified = System.currentTimeMillis()
                        )
                        personalTaskDao.updateTask(updatedEntity)

                        return Result.success(updatedEntity.toDomainModel())
                    } else {
                        Log.e(TAG, "Error creating task on server: ${response.errorBody()?.string()}")
                        // Vẫn trả về thành công vì đã lưu vào local database
                        return Result.success(taskEntity.toDomainModel())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing task to server", e)
                    // Không trả về lỗi vì đã lưu thành công vào local database
                    return Result.success(taskEntity.toDomainModel())
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
            // Lưu vào local database trước với trạng thái pending_update
            val taskEntity = task.toEntity().copy(
                syncStatus = "pending_update",
                lastModified = System.currentTimeMillis()
            )
            personalTaskDao.updateTask(taskEntity)

            // Nếu có kết nối mạng và task đã có serverId, đồng bộ lên server
            if (connectionChecker.isNetworkAvailable() && task.serverId != null) {
                try {
                    // Gọi API để cập nhật task trên server
                    val request = task.toApiRequest()
                    val response = apiService.updatePersonalTask(task.serverId, request)

                    if (response.isSuccessful && response.body() != null) {
                        // Cập nhật local database với thông tin từ server
                        val serverTask = response.body()!!
                        val updatedEntity = serverTask.toEntity(taskEntity).copy(
                            syncStatus = "synced",
                            lastModified = System.currentTimeMillis()
                        )
                        personalTaskDao.updateTask(updatedEntity)

                        return Result.success(updatedEntity.toDomainModel())
                    } else {
                        Log.e(TAG, "Error updating task on server: ${response.errorBody()?.string()}")
                        // Vẫn trả về thành công vì đã lưu vào local database
                        return Result.success(taskEntity.toDomainModel())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing task update to server", e)
                    // Không trả về lỗi vì đã lưu thành công vào local database
                    return Result.success(taskEntity.toDomainModel())
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

                    // Nếu có kết nối mạng, xóa trên server
                    if (connectionChecker.isNetworkAvailable()) {
                        try {
                            // Gọi API để xóa task trên server
                            val response = apiService.deletePersonalTask(task.serverId)

                            if (response.isSuccessful) {
                                // Xóa task khỏi local database
                                personalTaskDao.deleteTask(taskId)
                            } else {
                                Log.e(TAG, "Error deleting task on server: ${response.errorBody()?.string()}")
                                // Giữ trạng thái pending_delete để thử lại sau
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error syncing task deletion to server", e)
                            // Giữ trạng thái pending_delete để thử lại sau
                        }
                    }
                } else {
                    // Nếu task chưa được đồng bộ với server, xóa luôn
                    personalTaskDao.deleteLocalOnlyTask(taskId)
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
            // 1. Đẩy các thay đổi local lên server
            syncLocalChangesToServer()

            // 2. Lấy các thay đổi từ server về
            syncServerChangesToLocal()

            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing tasks", e)
            return Result.failure(e)
        }
    }

    private suspend fun syncLocalChangesToServer() {
        // Lấy các task đang chờ tạo mới
        val pendingCreateTasks = personalTaskDao.getTasksByStatus("pending_create")
        for (task in pendingCreateTasks) {
            try {
                val request = task.toDomainModel().toApiRequest()
                val response = apiService.createPersonalTask(request)

                if (response.isSuccessful && response.body() != null) {
                    val serverTask = response.body()!!
                    val updatedEntity = serverTask.toEntity(task).copy(
                        syncStatus = "synced",
                        lastModified = System.currentTimeMillis()
                    )
                    personalTaskDao.updateTask(updatedEntity)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing pending create task: ${task.id}", e)
            }
        }

        // Lấy các task đang chờ cập nhật
        val pendingUpdateTasks = personalTaskDao.getTasksByStatus("pending_update")
        for (task in pendingUpdateTasks) {
            if (task.serverId != null) {
                try {
                    val request = task.toDomainModel().toApiRequest()
                    val response = apiService.updatePersonalTask(task.serverId, request)

                    if (response.isSuccessful && response.body() != null) {
                        val serverTask = response.body()!!
                        val updatedEntity = serverTask.toEntity(task).copy(
                            syncStatus = "synced",
                            lastModified = System.currentTimeMillis()
                        )
                        personalTaskDao.updateTask(updatedEntity)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing pending update task: ${task.id}", e)
                }
            }
        }

        // Lấy các task đang chờ xóa
        val pendingDeleteTasks = personalTaskDao.getTasksByStatus("pending_delete")
        for (task in pendingDeleteTasks) {
            if (task.serverId != null) {
                try {
                    val response = apiService.deletePersonalTask(task.serverId)

                    if (response.isSuccessful) {
                        personalTaskDao.deleteTask(task.id)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing pending delete task: ${task.id}", e)
                }
            }
        }
    }

    private suspend fun syncServerChangesToLocal() {
        try {
            // Lấy tất cả task từ server
            val response = apiService.getPersonalTasks()

            if (response.isSuccessful && response.body() != null) {
                val serverTasksResponse = response.body()!!
                val serverTasks = serverTasksResponse.data

                // Lấy tất cả task từ local database
                val localTasks = personalTaskDao.getAllTasksSync()
                val localTaskMap = localTasks.associateBy { it.serverId }

                // Cập nhật hoặc thêm mới các task từ server
                for (serverTask in serverTasks) {
                    val localTask = localTaskMap[serverTask.id]

                    if (localTask != null) {
                        // Nếu task đã tồn tại trong local database và không đang chờ thay đổi
                        if (localTask.syncStatus == "synced") {
                            val updatedEntity = serverTask.toEntity(localTask)
                            personalTaskDao.updateTask(updatedEntity)
                        }
                    } else {
                        // Nếu task chưa tồn tại trong local database
                        val newEntity = serverTask.toEntity()
                        personalTaskDao.insertTask(newEntity)
                    }
                }

                // Xóa các task đã bị xóa trên server (không tồn tại trong danh sách server tasks)
                val serverTaskIds = serverTasks.map { it.id }.toSet()
                val tasksToDelete = localTasks.filter {
                    it.serverId != null &&
                    it.serverId !in serverTaskIds &&
                    it.syncStatus == "synced"
                }

                for (task in tasksToDelete) {
                    personalTaskDao.deleteTask(task.id)
                }

                // Nếu có phân trang, tiếp tục lấy các trang tiếp theo
                val meta = serverTasksResponse.meta
                if (meta != null && meta.current_page < meta.last_page) {
                    // Lấy các trang tiếp theo
                    for (page in meta.current_page + 1..meta.last_page) {
                        val nextPageResponse = apiService.getPersonalTasks(page = page)
                        if (nextPageResponse.isSuccessful && nextPageResponse.body() != null) {
                            val nextPageTasks = nextPageResponse.body()!!.data

                            for (serverTask in nextPageTasks) {
                                val localTask = localTaskMap[serverTask.id]

                                if (localTask != null) {
                                    // Nếu task đã tồn tại trong local database và không đang chờ thay đổi
                                    if (localTask.syncStatus == "synced") {
                                        val updatedEntity = serverTask.toEntity(localTask)
                                        personalTaskDao.updateTask(updatedEntity)
                                    }
                                } else {
                                    // Nếu task chưa tồn tại trong local database
                                    val newEntity = serverTask.toEntity()
                                    personalTaskDao.insertTask(newEntity)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing server changes to local", e)
            throw e
        }
    }
}
