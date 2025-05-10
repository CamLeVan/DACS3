package com.example.taskapplication.data.repository

import com.example.taskapplication.data.database.dao.DocumentDao
import com.example.taskapplication.data.database.dao.DocumentFolderDao
import android.content.Context
import com.example.taskapplication.data.database.dao.DocumentPermissionDao
import com.example.taskapplication.data.database.dao.DocumentVersionDao
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Singleton
import com.example.taskapplication.data.mapper.toEntity
import com.example.taskapplication.data.mapper.toDomain
import com.example.taskapplication.data.remote.ApiService
import com.example.taskapplication.data.remote.dto.CreateFolderRequest
import com.example.taskapplication.data.remote.dto.ResolveConflictsRequest
import com.example.taskapplication.data.remote.dto.UpdateDocumentRequest
import com.example.taskapplication.data.remote.dto.UpdateDocumentAccessRequest
import com.example.taskapplication.data.remote.dto.toDomain
import com.example.taskapplication.data.remote.dto.UpdateFolderRequest
import com.example.taskapplication.domain.model.Document
import com.example.taskapplication.domain.model.DocumentFolder
import com.example.taskapplication.domain.model.DocumentPermission
import com.example.taskapplication.domain.model.DocumentVersion
import com.example.taskapplication.domain.repository.DocumentRepository
import com.example.taskapplication.util.NetworkUtils
import com.example.taskapplication.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.UUID
import javax.inject.Inject

/**
 * Implementation of DocumentRepository
 */
@Singleton
class DocumentRepositoryImpl @Inject constructor(
    private val documentFolderDao: DocumentFolderDao,
    private val documentDao: DocumentDao,
    private val documentVersionDao: DocumentVersionDao,
    private val documentPermissionDao: DocumentPermissionDao,
    private val apiService: ApiService,
    private val networkUtils: NetworkUtils,
    @ApplicationContext private val context: Context
) : DocumentRepository {

    // Folders

    override fun getFoldersByTeam(teamId: String): Flow<Resource<List<DocumentFolder>>> {
        return flow {
            emit(Resource.Loading)

            try {
                // Đầu tiên, lấy dữ liệu từ cơ sở dữ liệu cục bộ
                val localFolders = documentFolderDao.getFoldersByTeam(teamId)
                emit(Resource.Success(localFolders.map { it.toDomain() }))

                // Nếu có kết nối mạng, lấy dữ liệu từ API
                if (networkUtils.isNetworkAvailable()) {
                    try {
                        val response = apiService.getTeamFolders(teamId)
                        if (response.isSuccessful && response.body() != null) {
                            val responseBody = response.body()!!
                            if (responseBody.data != null) {
                                val remoteFolders = responseBody.data.map { it.toDomain() }

                                // Lưu vào cơ sở dữ liệu cục bộ
                                for (folder in remoteFolders) {
                                    documentFolderDao.insertFolder(folder.toEntity())
                                }

                                // Phát lại dữ liệu mới
                                emit(Resource.Success(remoteFolders))
                            }
                        }
                    } catch (e: Exception) {
                        // Không phát lỗi nếu đã có dữ liệu cục bộ
                    }
                }
            } catch (e: Exception) {
                emit(Resource.Error("Không thể tải danh sách thư mục: ${e.message}"))
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun getSubfolders(folderId: String): Flow<Resource<List<DocumentFolder>>> {
        return flow {
            emit(Resource.Loading)

            try {
                // Lấy dữ liệu từ cơ sở dữ liệu cục bộ
                val localFolders = documentFolderDao.getSubfolders(folderId)
                emit(Resource.Success(localFolders.map { it.toDomain() }))

                // Nếu có kết nối mạng, lấy dữ liệu từ API
                if (networkUtils.isNetworkAvailable()) {
                    try {
                        val folderEntity = documentFolderDao.getFolderById(folderId)
                        if (folderEntity != null) {
                            val response = apiService.getTeamFolders(
                                teamId = folderEntity.teamId,
                                parentId = folderId
                            )

                            if (response.isSuccessful && response.body() != null) {
                                val responseBody = response.body()!!
                                if (responseBody.data != null) {
                                    val remoteFolders = responseBody.data.map { it.toDomain() }

                                    // Lưu vào cơ sở dữ liệu cục bộ
                                    for (folder in remoteFolders) {
                                        documentFolderDao.insertFolder(folder.toEntity())
                                    }

                                    // Phát lại dữ liệu mới
                                    emit(Resource.Success(remoteFolders))
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Không phát lỗi nếu đã có dữ liệu cục bộ
                    }
                }
            } catch (e: Exception) {
                emit(Resource.Error("Không thể tải danh sách thư mục con: ${e.message}"))
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun getRootFolders(teamId: String): Flow<Resource<List<DocumentFolder>>> {
        return flow {
            emit(Resource.Loading)

            try {
                // Lấy dữ liệu từ cơ sở dữ liệu cục bộ
                val localFolders = documentFolderDao.getRootFolders(teamId)
                emit(Resource.Success(localFolders.map { it.toDomain() }))

                // Nếu có kết nối mạng, lấy dữ liệu từ API
                if (networkUtils.isNetworkAvailable()) {
                    try {
                        val response = apiService.getTeamFolders(teamId, parentId = null)

                        if (response.isSuccessful && response.body() != null) {
                            val responseBody = response.body()!!
                            if (responseBody.data != null) {
                                val remoteFolders = responseBody.data.map { it.toDomain() }

                                // Lưu vào cơ sở dữ liệu cục bộ
                                for (folder in remoteFolders) {
                                    documentFolderDao.insertFolder(folder.toEntity())
                                }

                                // Phát lại dữ liệu mới
                                emit(Resource.Success(remoteFolders))
                            }
                        }
                    } catch (e: Exception) {
                        // Không phát lỗi nếu đã có dữ liệu cục bộ
                    }
                }
            } catch (e: Exception) {
                emit(Resource.Error("Không thể tải danh sách thư mục gốc: ${e.message}"))
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getFolderById(folderId: String): Resource<DocumentFolder> {
        return withContext(Dispatchers.IO) {
            try {
                // Đầu tiên, thử lấy từ cơ sở dữ liệu cục bộ
                val localFolder = documentFolderDao.getFolderById(folderId)
                if (localFolder != null) {
                    Resource.Success(localFolder.toDomain())
                } else if (networkUtils.isNetworkAvailable()) {
                    // Nếu không có cục bộ, thử lấy từ API
                    val response = apiService.getFolder(folderId)

                    if (response.isSuccessful && response.body() != null) {
                        val responseBody = response.body()!!
                        if (responseBody.data != null) {
                            val remoteFolder = responseBody.data.toDomain()

                            // Lưu vào cơ sở dữ liệu cục bộ
                            documentFolderDao.insertFolder(remoteFolder.toEntity())

                            Resource.Success(remoteFolder)
                        } else {
                            Resource.Error("Không thể tải thông tin thư mục: Dữ liệu không hợp lệ")
                        }
                    } else {
                        Resource.Error("Không thể tải thông tin thư mục: ${response.message()}")
                    }
                } else {
                    Resource.Error("Không thể tải thông tin thư mục: Không có kết nối mạng")
                }
            } catch (e: Exception) {
                Resource.Error("Không thể tải thông tin thư mục: ${e.message}")
            }
        }
    }

    override suspend fun createFolder(folder: DocumentFolder): Resource<DocumentFolder> {
        return withContext(Dispatchers.IO) {
            try {
                if (networkUtils.isNetworkAvailable()) {
                    // Tạo folder trên server trước
                    val request = CreateFolderRequest(
                        name = folder.name,
                        description = folder.description,
                        parentId = folder.parentFolderId?.toLongOrNull()
                    )

                    val response = apiService.createFolder(folder.teamId, request)

                    if (response.isSuccessful && response.body() != null) {
                        val responseBody = response.body()!!
                        if (responseBody.data != null) {
                            val remoteFolder = responseBody.data.toDomain()

                            // Lưu vào cơ sở dữ liệu cục bộ
                            documentFolderDao.insertFolder(remoteFolder.toEntity())

                            Resource.Success(remoteFolder)
                        } else {
                            Resource.Error("Không thể tạo thư mục: Dữ liệu không hợp lệ")
                        }
                    } else {
                        // Nếu API thất bại, tạo folder cục bộ với trạng thái "pending_create"
                        val localId = UUID.randomUUID().toString()
                        val localFolder = folder.copy(
                            id = localId,
                            syncStatus = "pending_create"
                        )

                        documentFolderDao.insertFolder(localFolder.toEntity())

                        Resource.Success(localFolder)
                    }
                } else {
                    // Nếu không có mạng, tạo folder cục bộ với trạng thái "pending_create"
                    val localId = UUID.randomUUID().toString()
                    val localFolder = folder.copy(
                        id = localId,
                        syncStatus = "pending_create"
                    )

                    documentFolderDao.insertFolder(localFolder.toEntity())

                    Resource.Success(localFolder)
                }
            } catch (e: Exception) {
                Resource.Error("Không thể tạo thư mục: ${e.message}")
            }
        }
    }
    override suspend fun updateFolder(folder: DocumentFolder): Resource<DocumentFolder> {
        return withContext(Dispatchers.IO) {
            try {
                if (networkUtils.isNetworkAvailable()) {
                    // Cập nhật folder trên server trước
                    val request = UpdateFolderRequest(
                        name = folder.name,
                        description = folder.description
                    )

                    val response = apiService.updateFolder(folder.id, request)

                    if (response.isSuccessful && response.body() != null) {
                        val responseBody = response.body()!!
                        if (responseBody.data != null) {
                            val remoteFolder = responseBody.data.toDomain()

                            // Cập nhật trong cơ sở dữ liệu cục bộ
                            documentFolderDao.updateFolder(remoteFolder.toEntity())

                            Resource.Success(remoteFolder)
                        } else {
                            Resource.Error("Không thể cập nhật thư mục: Dữ liệu không hợp lệ")
                        }
                    } else {
                        // Nếu API thất bại, cập nhật folder cục bộ với trạng thái "pending_update"
                        val localFolder = folder.copy(syncStatus = "pending_update")
                        documentFolderDao.updateFolder(localFolder.toEntity())

                        Resource.Success(localFolder)
                    }
                } else {
                    // Nếu không có mạng, cập nhật folder cục bộ với trạng thái "pending_update"
                    val localFolder = folder.copy(syncStatus = "pending_update")
                    documentFolderDao.updateFolder(localFolder.toEntity())

                    Resource.Success(localFolder)
                }
            } catch (e: Exception) {
                Resource.Error("Không thể cập nhật thư mục: ${e.message}")
            }
        }
    }
}