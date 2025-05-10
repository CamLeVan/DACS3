package com.example.taskapplication.data.repository

import android.content.Context
import android.net.Uri
import com.example.taskapplication.data.database.dao.DocumentDao
import com.example.taskapplication.data.database.dao.DocumentFolderDao
import com.example.taskapplication.data.database.dao.DocumentPermissionDao
import com.example.taskapplication.data.database.dao.DocumentVersionDao
import com.example.taskapplication.data.mapper.toDomain
import com.example.taskapplication.data.mapper.toEntity
import com.example.taskapplication.data.remote.ApiService
import com.example.taskapplication.data.remote.dto.CreateFolderRequest
import com.example.taskapplication.data.remote.dto.UpdateDocumentAccessRequest
import com.example.taskapplication.data.remote.dto.UpdateDocumentRequest
import com.example.taskapplication.data.remote.dto.UpdateFolderRequest
import com.example.taskapplication.data.remote.dto.toDomain
import com.example.taskapplication.domain.model.Document
import com.example.taskapplication.domain.model.DocumentFolder
import com.example.taskapplication.domain.model.DocumentPermission
import com.example.taskapplication.domain.model.DocumentVersion
import com.example.taskapplication.domain.repository.DocumentRepository
import com.example.taskapplication.util.NetworkUtils
import com.example.taskapplication.util.Resource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

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

    // Document Folders

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
                            val remoteFolders = response.body()!!.data?.map { it.toDomain() } ?: emptyList()

                            // Lưu vào cơ sở dữ liệu cục bộ
                            for (folder in remoteFolders) {
                                documentFolderDao.insertFolder(folder.toEntity())
                            }

                            // Phát lại dữ liệu mới
                            emit(Resource.Success(remoteFolders))
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
                        val response = apiService.getTeamFolders(
                            // Cần lấy teamId từ folderId, giả sử đã có phương thức này
                            teamId = documentFolderDao.getFolderById(folderId)?.teamId ?: "",
                            parentId = folderId
                        )

                        if (response.isSuccessful && response.body() != null) {
                            val remoteFolders = response.body()!!.data?.map { it.toDomain() } ?: emptyList()

                            // Lưu vào cơ sở dữ liệu cục bộ
                            for (folder in remoteFolders) {
                                documentFolderDao.insertFolder(folder.toEntity())
                            }

                            // Phát lại dữ liệu mới
                            emit(Resource.Success(remoteFolders))
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
                            val remoteFolders = response.body()!!.data?.map { it.toDomain() } ?: emptyList()

                            // Lưu vào cơ sở dữ liệu cục bộ
                            for (folder in remoteFolders) {
                                documentFolderDao.insertFolder(folder.toEntity())
                            }

                            // Phát lại dữ liệu mới
                            emit(Resource.Success(remoteFolders))
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
                        val remoteFolder = response.body()!!.data?.toDomain() ?: return@withContext Resource.Error("Không thể tải thông tin thư mục: Dữ liệu không hợp lệ")

                        // Lưu vào cơ sở dữ liệu cục bộ
                        documentFolderDao.insertFolder(remoteFolder.toEntity())

                        Resource.Success(remoteFolder)
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
                        val remoteFolder = response.body()!!.data?.toDomain() ?: return@withContext Resource.Error("Không thể tạo thư mục: Dữ liệu không hợp lệ")

                        // Lưu vào cơ sở dữ liệu cục bộ
                        documentFolderDao.insertFolder(remoteFolder.toEntity())

                        Resource.Success(remoteFolder)
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
                        val remoteFolder = response.body()!!.data?.toDomain() ?: return@withContext Resource.Error("Không thể cập nhật thư mục: Dữ liệu không hợp lệ")

                        // Cập nhật trong cơ sở dữ liệu cục bộ
                        documentFolderDao.updateFolder(remoteFolder.toEntity())

                        Resource.Success(remoteFolder)
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

    override suspend fun deleteFolder(folderId: String): Resource<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (networkUtils.isNetworkAvailable()) {
                    // Xóa folder trên server trước
                    val response = apiService.deleteFolder(folderId)

                    if (response.isSuccessful) {
                        // Xóa trong cơ sở dữ liệu cục bộ
                        val folder = documentFolderDao.getFolderById(folderId)
                        if (folder != null) {
                            documentFolderDao.deleteFolder(folderId)
                        }

                        Resource.Success(Unit)
                    } else {
                        // Nếu API thất bại, đánh dấu folder cục bộ với trạng thái "pending_delete"
                        val folder = documentFolderDao.getFolderById(folderId)
                        if (folder != null) {
                            val updatedFolder = folder.copy(
                                isDeleted = true,
                                syncStatus = "pending_delete"
                            )
                            documentFolderDao.updateFolder(updatedFolder)
                        }

                        Resource.Success(Unit)
                    }
                } else {
                    // Nếu không có mạng, đánh dấu folder cục bộ với trạng thái "pending_delete"
                    val folder = documentFolderDao.getFolderById(folderId)
                    if (folder != null) {
                        val updatedFolder = folder.copy(
                            isDeleted = true,
                            syncStatus = "pending_delete"
                        )
                        documentFolderDao.updateFolder(updatedFolder)
                    }

                    Resource.Success(Unit)
                }
            } catch (e: Exception) {
                Resource.Error("Không thể xóa thư mục: ${e.message}")
            }
        }
    }

    // Documents

    override fun getDocumentsByTeam(teamId: String): Flow<Resource<List<Document>>> {
        return flow {
            emit(Resource.Loading)

            try {
                // Đầu tiên, lấy dữ liệu từ cơ sở dữ liệu cục bộ
                val localDocuments = documentDao.getDocumentsByTeam(teamId)
                emit(Resource.Success(localDocuments.map { it.toDomain() }))

                // Nếu có kết nối mạng, lấy dữ liệu từ API
                if (networkUtils.isNetworkAvailable()) {
                    try {
                        val response = apiService.getTeamDocuments(teamId)
                        if (response.isSuccessful && response.body() != null) {
                            val remoteDocuments = response.body()!!.data?.map { it.toDomain() } ?: emptyList()

                            // Lưu vào cơ sở dữ liệu cục bộ
                            for (document in remoteDocuments) {
                                documentDao.insertDocument(document.toEntity())
                            }

                            // Phát lại dữ liệu mới
                            emit(Resource.Success(remoteDocuments))
                        }
                    } catch (e: Exception) {
                        // Không phát lỗi nếu đã có dữ liệu cục bộ
                    }
                }
            } catch (e: Exception) {
                emit(Resource.Error("Không thể tải danh sách tài liệu: ${e.message}"))
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun getDocumentsByFolder(folderId: String): Flow<Resource<List<Document>>> {
        return flow {
            emit(Resource.Loading)

            try {
                // Lấy dữ liệu từ cơ sở dữ liệu cục bộ
                val localDocuments = documentDao.getDocumentsByFolder(folderId)
                emit(Resource.Success(localDocuments.map { it.toDomain() }))

                // Nếu có kết nối mạng, lấy dữ liệu từ API
                if (networkUtils.isNetworkAvailable()) {
                    try {
                        // Cần lấy teamId từ folderId
                        val folder = documentFolderDao.getFolderById(folderId)
                        if (folder != null) {
                            val response = apiService.getTeamDocuments(
                                teamId = folder.teamId,
                                folderId = folderId
                            )

                            if (response.isSuccessful && response.body() != null) {
                                val remoteDocuments = response.body()!!.data?.map { it.toDomain() } ?: emptyList()

                                // Lưu vào cơ sở dữ liệu cục bộ
                                for (document in remoteDocuments) {
                                    documentDao.insertDocument(document.toEntity())
                                }

                                // Phát lại dữ liệu mới
                                emit(Resource.Success(remoteDocuments))
                            }
                        }
                    } catch (e: Exception) {
                        // Không phát lỗi nếu đã có dữ liệu cục bộ
                    }
                }
            } catch (e: Exception) {
                emit(Resource.Error("Không thể tải danh sách tài liệu trong thư mục: ${e.message}"))
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun getRootDocuments(teamId: String): Flow<Resource<List<Document>>> {
        return flow {
            emit(Resource.Loading)

            try {
                // Lấy dữ liệu từ cơ sở dữ liệu cục bộ
                val localDocuments = documentDao.getRootDocuments(teamId)
                emit(Resource.Success(localDocuments.map { it.toDomain() }))

                // Nếu có kết nối mạng, lấy dữ liệu từ API
                if (networkUtils.isNetworkAvailable()) {
                    try {
                        val response = apiService.getTeamDocuments(teamId, folderId = null)

                        if (response.isSuccessful && response.body() != null) {
                            val remoteDocuments = response.body()!!.data?.map { it.toDomain() } ?: emptyList()

                            // Lưu vào cơ sở dữ liệu cục bộ
                            for (document in remoteDocuments) {
                                documentDao.insertDocument(document.toEntity())
                            }

                            // Phát lại dữ liệu mới
                            emit(Resource.Success(remoteDocuments))
                        }
                    } catch (e: Exception) {
                        // Không phát lỗi nếu đã có dữ liệu cục bộ
                    }
                }
            } catch (e: Exception) {
                emit(Resource.Error("Không thể tải danh sách tài liệu gốc: ${e.message}"))
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getDocumentById(documentId: String): Resource<Document> {
        return withContext(Dispatchers.IO) {
            try {
                // Đầu tiên, thử lấy từ cơ sở dữ liệu cục bộ
                val localDocument = documentDao.getDocumentById(documentId)
                if (localDocument != null) {
                    Resource.Success(localDocument.toDomain())
                } else if (networkUtils.isNetworkAvailable()) {
                    // Nếu không có cục bộ, thử lấy từ API
                    val response = apiService.getDocument(documentId)

                    if (response.isSuccessful && response.body() != null) {
                        val remoteDocument = response.body()!!.data?.toDomain() ?: return@withContext Resource.Error("Không thể tải thông tin tài liệu: Dữ liệu không hợp lệ")

                        // Lưu vào cơ sở dữ liệu cục bộ
                        documentDao.insertDocument(remoteDocument.toEntity())

                        Resource.Success(remoteDocument)
                    } else {
                        Resource.Error("Không thể tải thông tin tài liệu: ${response.message()}")
                    }
                } else {
                    Resource.Error("Không thể tải thông tin tài liệu: Không có kết nối mạng")
                }
            } catch (e: Exception) {
                Resource.Error("Không thể tải thông tin tài liệu: ${e.message}")
            }
        }
    }

    override fun getDocumentByIdFlow(documentId: String): Flow<Resource<Document>> {
        return flow {
            emit(Resource.Loading)

            try {
                // Đầu tiên, lấy từ cơ sở dữ liệu cục bộ
                val localDocument = documentDao.getDocumentByIdFlow(documentId)

                // Phát dữ liệu cục bộ
                localDocument.collect { entity ->
                    if (entity != null) {
                        emit(Resource.Success(entity.toDomain()))
                    }
                }

                // Nếu có kết nối mạng, lấy dữ liệu từ API
                if (networkUtils.isNetworkAvailable()) {
                    try {
                        val response = apiService.getDocument(documentId)

                        if (response.isSuccessful && response.body() != null) {
                            val remoteDocument = response.body()!!.data?.toDomain() ?: return@flow

                            // Lưu vào cơ sở dữ liệu cục bộ
                            documentDao.insertDocument(remoteDocument.toEntity())

                            // Không cần phát lại vì Flow từ Room sẽ tự động phát khi dữ liệu thay đổi
                        }
                    } catch (e: Exception) {
                        // Không phát lỗi nếu đã có dữ liệu cục bộ
                    }
                }
            } catch (e: Exception) {
                emit(Resource.Error("Không thể tải thông tin tài liệu: ${e.message}"))
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun createDocument(document: Document, file: File): Resource<Document> {
        return withContext(Dispatchers.IO) {
            try {
                if (networkUtils.isNetworkAvailable()) {
                    // Tải lên tài liệu lên server
                    val fileRequestBody = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                    val filePart = MultipartBody.Part.createFormData("file", file.name, fileRequestBody)

                    // Tạo các phần khác của request
                    val namePart = document.name.toRequestBody("text/plain".toMediaTypeOrNull())
                    val descriptionPart = document.description?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val folderIdPart = document.folderId?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val accessLevelPart = document.accessLevel.toRequestBody("text/plain".toMediaTypeOrNull())
                    val allowedUsersPart = document.allowedUsers.joinToString(",").toRequestBody("text/plain".toMediaTypeOrNull())

                    val response = apiService.uploadDocument(
                        teamId = document.teamId,
                        file = filePart,
                        name = namePart,
                        description = descriptionPart,
                        folderId = folderIdPart,
                        accessLevel = accessLevelPart,
                        allowedUsers = allowedUsersPart
                    )

                    if (response.isSuccessful && response.body() != null) {
                        val remoteDocument = response.body()!!.data?.toDomain() ?: return@withContext Resource.Error("Không thể tải lên tài liệu: Dữ liệu không hợp lệ")

                        // Lưu vào cơ sở dữ liệu cục bộ
                        documentDao.insertDocument(remoteDocument.toEntity())

                        // Lưu file vào bộ nhớ cục bộ
                        val localFile = saveFileLocally(file, remoteDocument.id)

                        Resource.Success(remoteDocument.copy(fileUrl = localFile.absolutePath))
                    } else {
                        // Nếu API thất bại, tạo tài liệu cục bộ với trạng thái "pending_create"
                        val localId = UUID.randomUUID().toString()
                        val localFile = saveFileLocally(file, localId)

                        val localDocument = document.copy(
                            id = localId,
                            fileUrl = localFile.absolutePath,
                            syncStatus = "pending_create"
                        )

                        documentDao.insertDocument(localDocument.toEntity())

                        Resource.Success(localDocument)
                    }
                } else {
                    // Nếu không có mạng, tạo tài liệu cục bộ với trạng thái "pending_create"
                    val localId = UUID.randomUUID().toString()
                    val localFile = saveFileLocally(file, localId)

                    val localDocument = document.copy(
                        id = localId,
                        fileUrl = localFile.absolutePath,
                        syncStatus = "pending_create"
                    )

                    documentDao.insertDocument(localDocument.toEntity())

                    Resource.Success(localDocument)
                }
            } catch (e: Exception) {
                Resource.Error("Không thể tạo tài liệu: ${e.message}")
            }
        }
    }

    override suspend fun updateDocument(document: Document): Resource<Document> {
        return withContext(Dispatchers.IO) {
            try {
                if (networkUtils.isNetworkAvailable()) {
                    // Cập nhật tài liệu trên server
                    val request = UpdateDocumentRequest(
                        name = document.name,
                        description = document.description,
                        folderId = document.folderId?.toLongOrNull()
                    )

                    val response = apiService.updateDocument(document.id, request)

                    if (response.isSuccessful && response.body() != null) {
                        val remoteDocument = response.body()!!.data.toDomain()

                        // Cập nhật trong cơ sở dữ liệu cục bộ
                        documentDao.updateDocument(remoteDocument.toEntity())

                        Resource.Success(remoteDocument)
                    } else {
                        // Nếu API thất bại, cập nhật tài liệu cục bộ với trạng thái "pending_update"
                        val localDocument = document.copy(syncStatus = "pending_update")
                        documentDao.updateDocument(localDocument.toEntity())

                        Resource.Success(localDocument)
                    }
                } else {
                    // Nếu không có mạng, cập nhật tài liệu cục bộ với trạng thái "pending_update"
                    val localDocument = document.copy(syncStatus = "pending_update")
                    documentDao.updateDocument(localDocument.toEntity())

                    Resource.Success(localDocument)
                }
            } catch (e: Exception) {
                Resource.Error("Không thể cập nhật tài liệu: ${e.message}")
            }
        }
    }

    override suspend fun deleteDocument(documentId: String): Resource<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (networkUtils.isNetworkAvailable()) {
                    // Xóa tài liệu trên server
                    val response = apiService.deleteDocument(documentId)

                    if (response.isSuccessful) {
                        // Xóa trong cơ sở dữ liệu cục bộ
                        val document = documentDao.getDocumentById(documentId)
                        if (document != null) {
                            documentDao.deleteDocument(documentId)
                        }

                        Resource.Success(Unit)
                    } else {
                        // Nếu API thất bại, đánh dấu tài liệu cục bộ với trạng thái "pending_delete"
                        val document = documentDao.getDocumentById(documentId)
                        if (document != null) {
                            val updatedDocument = document.copy(
                                isDeleted = true,
                                syncStatus = "pending_delete"
                            )
                            documentDao.updateDocument(updatedDocument)
                        }

                        Resource.Success(Unit)
                    }
                } else {
                    // Nếu không có mạng, đánh dấu tài liệu cục bộ với trạng thái "pending_delete"
                    val document = documentDao.getDocumentById(documentId)
                    if (document != null) {
                        val updatedDocument = document.copy(
                            isDeleted = true,
                            syncStatus = "pending_delete"
                        )
                        documentDao.updateDocument(updatedDocument)
                    }

                    Resource.Success(Unit)
                }
            } catch (e: Exception) {
                Resource.Error("Không thể xóa tài liệu: ${e.message}")
            }
        }
    }

    override suspend fun downloadDocument(documentId: String, versionId: String?): Resource<File> {
        return withContext(Dispatchers.IO) {
            try {
                // Kiểm tra xem tài liệu đã có cục bộ chưa
                val document = documentDao.getDocumentById(documentId)
                if (document != null && File(document.fileUrl).exists()) {
                    return@withContext Resource.Success(File(document.fileUrl))
                }

                // Nếu không có cục bộ hoặc file không tồn tại, tải từ server
                if (networkUtils.isNetworkAvailable()) {
                    val response = if (versionId != null) {
                        // Tải phiên bản cụ thể
                        val version = documentVersionDao.getVersionById(versionId)
                        if (version != null) {
                            apiService.downloadVersion(documentId, version.versionNumber)
                        } else {
                            apiService.downloadDocument(documentId)
                        }
                    } else {
                        // Tải phiên bản mới nhất
                        apiService.downloadDocument(documentId)
                    }

                    if (response.isSuccessful && response.body() != null) {
                        // Lưu file vào bộ nhớ cục bộ
                        val fileName = if (versionId != null) "${documentId}_${versionId}" else documentId
                        val file = saveFileFromResponse(response.body()!!, fileName)

                        // Cập nhật đường dẫn file trong cơ sở dữ liệu
                        if (document != null) {
                            documentDao.updateDocument(document.copy(fileUrl = file.absolutePath))
                        }

                        Resource.Success(file)
                    } else {
                        Resource.Error("Không thể tải xuống tài liệu: ${response.message()}")
                    }
                } else {
                    Resource.Error("Không thể tải xuống tài liệu: Không có kết nối mạng")
                }
            } catch (e: Exception) {
                Resource.Error("Không thể tải xuống tài liệu: ${e.message}")
            }
        }
    }

    override fun searchDocuments(teamId: String, query: String): Flow<Resource<List<Document>>> {
        return flow {
            emit(Resource.Loading)

            try {
                // Tìm kiếm cục bộ trước
                val localResults = documentDao.searchDocuments(teamId, query)
                emit(Resource.Success(localResults.map { it.toDomain() }))

                // Nếu có kết nối mạng, tìm kiếm trên server
                if (networkUtils.isNetworkAvailable()) {
                    try {
                        val response = apiService.getTeamDocuments(teamId, search = query)

                        if (response.isSuccessful && response.body() != null) {
                            val remoteResults = response.body()!!.data.map { it.toDomain() }

                            // Lưu vào cơ sở dữ liệu cục bộ
                            remoteResults.forEach { document ->
                                documentDao.insertDocument(document.toEntity())
                            }

                            // Phát lại dữ liệu mới
                            emit(Resource.Success(remoteResults))
                        }
                    } catch (e: Exception) {
                        // Không phát lỗi nếu đã có dữ liệu cục bộ
                    }
                }
            } catch (e: Exception) {
                emit(Resource.Error("Không thể tìm kiếm tài liệu: ${e.message}"))
            }
        }.flowOn(Dispatchers.IO)
    }

    // Document Versions

    override fun getVersionsByDocument(documentId: String): Flow<Resource<List<DocumentVersion>>> {
        return flow { emit(Resource.Success(emptyList())) }
    }

    override suspend fun getVersionById(versionId: String): Resource<DocumentVersion> {
        return Resource.Error("Not implemented")
    }

    override suspend fun getLatestVersion(documentId: String): Resource<DocumentVersion> {
        return Resource.Error("Not implemented")
    }

    override suspend fun createVersion(version: DocumentVersion, file: File): Resource<DocumentVersion> {
        return Resource.Error("Not implemented")
    }

    // Document Permissions

    override fun getPermissionsByDocument(documentId: String): Flow<Resource<List<DocumentPermission>>> {
        return flow { emit(Resource.Success(emptyList())) }
    }

    override suspend fun getUserPermission(documentId: String, userId: String): Resource<DocumentPermission> {
        return Resource.Error("Not implemented")
    }

    override suspend fun createPermission(permission: DocumentPermission): Resource<DocumentPermission> {
        return Resource.Error("Not implemented")
    }

    override suspend fun deletePermission(documentId: String, userId: String): Resource<Unit> {
        return Resource.Error("Not implemented")
    }

    // Synchronization

    override suspend fun syncDocuments(): Resource<Unit> {
        return Resource.Error("Not implemented")
    }

    // Utility functions

    private fun saveFileLocally(file: File, documentId: String): File {
        val documentsDir = File(context.filesDir, "documents")
        if (!documentsDir.exists()) {
            documentsDir.mkdirs()
        }

        val destinationFile = File(documentsDir, "${documentId}_${file.name}")
        file.copyTo(destinationFile, overwrite = true)
        return destinationFile
    }

    private fun saveFileFromResponse(responseBody: okhttp3.ResponseBody, fileName: String): File {
        val documentsDir = File(context.filesDir, "documents")
        if (!documentsDir.exists()) {
            documentsDir.mkdirs()
        }

        val file = File(documentsDir, fileName)
        val inputStream = responseBody.byteStream()
        val outputStream = FileOutputStream(file)

        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }

        return file
    }
}
