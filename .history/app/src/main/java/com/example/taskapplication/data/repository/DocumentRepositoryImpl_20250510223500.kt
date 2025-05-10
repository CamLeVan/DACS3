package com.example.taskapplication.data.repository

import android.content.Context
import com.example.taskapplication.data.database.dao.DocumentDao
import com.example.taskapplication.data.database.dao.DocumentFolderDao
import com.example.taskapplication.data.database.dao.DocumentPermissionDao
import com.example.taskapplication.data.database.dao.DocumentVersionDao
import com.example.taskapplication.data.mapper.toDomain
import com.example.taskapplication.data.mapper.toEntity
import com.example.taskapplication.data.remote.ApiService
import com.example.taskapplication.data.remote.api.DocumentApiService
import com.example.taskapplication.data.remote.request.CreateDocumentVersionRequest
import com.example.taskapplication.data.remote.request.CreateDocumentPermissionRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import java.io.File
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
    private val documentApiService: DocumentApiService,
    private val networkUtils: NetworkUtils,
    @ApplicationContext private val context: Context
) : DocumentRepository {

    // Document Folders
    override fun getFoldersByTeam(teamId: String): Flow<Resource<List<DocumentFolder>>> {
        return flow {
            emit(Resource.Loading)
            try {
                val localFolders = documentFolderDao.getFoldersByTeam(teamId)
                emit(Resource.Success(localFolders.map { it.toDomain() }))
            } catch (e: Exception) {
                emit(Resource.Error("Không thể tải danh sách thư mục: ${e.message}"))
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun getSubfolders(folderId: String): Flow<Resource<List<DocumentFolder>>> {
        return flow {
            emit(Resource.Loading)
            try {
                val localFolders = documentFolderDao.getSubfolders(folderId)
                emit(Resource.Success(localFolders.map { it.toDomain() }))
            } catch (e: Exception) {
                emit(Resource.Error("Không thể tải danh sách thư mục con: ${e.message}"))
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun getRootFolders(teamId: String): Flow<Resource<List<DocumentFolder>>> {
        return flow {
            emit(Resource.Loading)
            try {
                val localFolders = documentFolderDao.getRootFolders(teamId)
                emit(Resource.Success(localFolders.map { it.toDomain() }))
            } catch (e: Exception) {
                emit(Resource.Error("Không thể tải danh sách thư mục gốc: ${e.message}"))
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getFolderById(folderId: String): Resource<DocumentFolder> {
        return withContext(Dispatchers.IO) {
            try {
                val localFolder = documentFolderDao.getFolderById(folderId)
                if (localFolder != null) {
                    return@withContext Resource.Success(localFolder.toDomain())
                } else {
                    return@withContext Resource.Error("Không tìm thấy thư mục")
                }
            } catch (e: Exception) {
                return@withContext Resource.Error("Không thể tải thông tin thư mục: ${e.message}")
            }
        }
    }

    override suspend fun createFolder(folder: DocumentFolder): Resource<DocumentFolder> {
        return withContext(Dispatchers.IO) {
            try {
                val localId = UUID.randomUUID().toString()
                val localFolder = folder.copy(
                    id = localId,
                    syncStatus = "pending_create"
                )
                documentFolderDao.insertFolder(localFolder.toEntity())
                return@withContext Resource.Success(localFolder)
            } catch (e: Exception) {
                return@withContext Resource.Error("Không thể tạo thư mục: ${e.message}")
            }
        }
    }

    override suspend fun updateFolder(folder: DocumentFolder): Resource<DocumentFolder> {
        return withContext(Dispatchers.IO) {
            try {
                val localFolder = folder.copy(syncStatus = "pending_update")
                documentFolderDao.updateFolder(localFolder.toEntity())
                return@withContext Resource.Success(localFolder)
            } catch (e: Exception) {
                return@withContext Resource.Error("Không thể cập nhật thư mục: ${e.message}")
            }
        }
    }

    override suspend fun deleteFolder(folderId: String): Resource<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                documentFolderDao.deleteFolder(folderId)
                return@withContext Resource.Success(Unit)
            } catch (e: Exception) {
                return@withContext Resource.Error("Không thể xóa thư mục: ${e.message}")
            }
        }
    }

    // Documents
    override fun getDocumentsByTeam(teamId: String): Flow<Resource<List<Document>>> {
        return flow {
            emit(Resource.Loading)
            try {
                val localDocuments = documentDao.getDocumentsByTeam(teamId)
                emit(Resource.Success(localDocuments.map { it.toDomain() }))
            } catch (e: Exception) {
                emit(Resource.Error("Không thể tải danh sách tài liệu: ${e.message}"))
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun getDocumentsByFolder(folderId: String): Flow<Resource<List<Document>>> {
        return flow {
            emit(Resource.Loading)
            try {
                val localDocuments = documentDao.getDocumentsByFolder(folderId)
                emit(Resource.Success(localDocuments.map { it.toDomain() }))
            } catch (e: Exception) {
                emit(Resource.Error("Không thể tải danh sách tài liệu trong thư mục: ${e.message}"))
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun getRootDocuments(teamId: String): Flow<Resource<List<Document>>> {
        return flow {
            emit(Resource.Loading)
            try {
                val localDocuments = documentDao.getRootDocuments(teamId)
                emit(Resource.Success(localDocuments.map { it.toDomain() }))
            } catch (e: Exception) {
                emit(Resource.Error("Không thể tải danh sách tài liệu gốc: ${e.message}"))
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getDocumentById(documentId: String): Resource<Document> {
        return withContext(Dispatchers.IO) {
            try {
                val localDocument = documentDao.getDocumentById(documentId)
                if (localDocument != null) {
                    return@withContext Resource.Success(localDocument.toDomain())
                } else {
                    return@withContext Resource.Error("Không tìm thấy tài liệu")
                }
            } catch (e: Exception) {
                return@withContext Resource.Error("Không thể tải thông tin tài liệu: ${e.message}")
            }
        }
    }

    override fun getDocumentByIdFlow(documentId: String): Flow<Resource<Document>> {
        return flow {
            emit(Resource.Loading)
            try {
                documentDao.getDocumentByIdFlow(documentId).collect { entity ->
                    if (entity != null) {
                        emit(Resource.Success(entity.toDomain()))
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
                val localId = UUID.randomUUID().toString()
                val localDocument = document.copy(
                    id = localId,
                    syncStatus = "pending_create"
                )
                documentDao.insertDocument(localDocument.toEntity())
                return@withContext Resource.Success(localDocument)
            } catch (e: Exception) {
                return@withContext Resource.Error("Không thể tạo tài liệu: ${e.message}")
            }
        }
    }

    override suspend fun updateDocument(document: Document): Resource<Document> {
        return withContext(Dispatchers.IO) {
            try {
                val localDocument = document.copy(syncStatus = "pending_update")
                documentDao.updateDocument(localDocument.toEntity())
                return@withContext Resource.Success(localDocument)
            } catch (e: Exception) {
                return@withContext Resource.Error("Không thể cập nhật tài liệu: ${e.message}")
            }
        }
    }

    override suspend fun deleteDocument(documentId: String): Resource<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                documentDao.deleteDocument(documentId)
                return@withContext Resource.Success(Unit)
            } catch (e: Exception) {
                return@withContext Resource.Error("Không thể xóa tài liệu: ${e.message}")
            }
        }
    }

    override suspend fun downloadDocument(documentId: String, versionId: String?): Resource<File> {
        return withContext(Dispatchers.IO) {
            try {
                if (!networkUtils.isNetworkAvailable()) {
                    return@withContext Resource.Error("Không có kết nối mạng")
                }

                val response = if (versionId != null) {
                    // Tải phiên bản cụ thể
                    val version = documentVersionDao.getVersionById(versionId)
                    if (version == null) {
                        return@withContext Resource.Error("Không tìm thấy phiên bản")
                    }
                    apiService.downloadVersion(documentId, version.versionNumber)
                } else {
                    // Tải phiên bản mới nhất
                    apiService.downloadDocument(documentId)
                }

                if (!response.isSuccessful || response.body() == null) {
                    return@withContext Resource.Error("Không thể tải tài liệu: ${response.message()}")
                }

                // Lưu file vào bộ nhớ cục bộ
                val responseBody = response.body()!!
                val tempFile = File.createTempFile("download_", ".tmp", context.cacheDir)

                tempFile.outputStream().use { fileOutputStream ->
                    responseBody.byteStream().use { inputStream ->
                        inputStream.copyTo(fileOutputStream)
                    }
                }

                // Lưu vào thư mục documents
                val savedFile = saveFileLocally(tempFile, documentId)
                tempFile.delete() // Xóa file tạm

                return@withContext Resource.Success(savedFile)
            } catch (e: Exception) {
                return@withContext Resource.Error("Lỗi khi tải tài liệu: ${e.message}")
            }
        }
    }

    override fun searchDocuments(teamId: String, query: String): Flow<Resource<List<Document>>> {
        return flow {
            emit(Resource.Loading)
            try {
                documentDao.searchDocuments(teamId, query).collect { localResults ->
                    emit(Resource.Success(localResults.map { it.toDomain() }))
                }
            } catch (e: Exception) {
                emit(Resource.Error("Không thể tìm kiếm tài liệu: ${e.message}"))
            }
        }.flowOn(Dispatchers.IO)
    }

    // Document Versions
    override fun getVersionsByDocument(documentId: String): Flow<Resource<List<DocumentVersion>>> {
        return flow {
            emit(Resource.Loading)
            try {
                documentVersionDao.getVersionsByDocument(documentId).collect { localVersions ->
                    emit(Resource.Success(localVersions.map { it.toDomain() }))
                }
            } catch (e: Exception) {
                emit(Resource.Error("Không thể tải danh sách phiên bản: ${e.message}"))
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getVersionById(versionId: String): Resource<DocumentVersion> {
        return withContext(Dispatchers.IO) {
            try {
                val localVersion = documentVersionDao.getVersionById(versionId)
                if (localVersion != null) {
                    return@withContext Resource.Success(localVersion.toDomain())
                } else {
                    return@withContext Resource.Error("Không tìm thấy phiên bản")
                }
            } catch (e: Exception) {
                return@withContext Resource.Error("Không thể tải thông tin phiên bản: ${e.message}")
            }
        }
    }

    override suspend fun getLatestVersion(documentId: String): Resource<DocumentVersion> {
        return withContext(Dispatchers.IO) {
            try {
                val document = documentDao.getDocumentById(documentId)
                if (document == null) {
                    return@withContext Resource.Error("Không tìm thấy tài liệu")
                }

                val latestVersionId = document.latestVersionId
                if (latestVersionId == null) {
                    return@withContext Resource.Error("Tài liệu chưa có phiên bản nào")
                }

                val latestVersion = documentVersionDao.getVersionById(latestVersionId)
                if (latestVersion != null) {
                    return@withContext Resource.Success(latestVersion.toDomain())
                } else {
                    return@withContext Resource.Error("Không tìm thấy phiên bản mới nhất")
                }
            } catch (e: Exception) {
                return@withContext Resource.Error("Không thể tải phiên bản mới nhất: ${e.message}")
            }
        }
    }

    override suspend fun createVersion(version: DocumentVersion, file: File): Resource<DocumentVersion> {
        return withContext(Dispatchers.IO) {
            try {
                // Lấy số phiên bản tiếp theo
                val versions = documentVersionDao.getVersionsByDocument(version.documentId)
                val nextVersionNumber = versions.size + 1

                // Tạo phiên bản mới cục bộ
                val localId = UUID.randomUUID().toString()
                val localVersion = version.copy(
                    id = localId,
                    versionNumber = nextVersionNumber,
                    fileSize = file.length(),
                    uploadedAt = System.currentTimeMillis(),
                    syncStatus = "pending_create"
                )

                // Lưu vào cơ sở dữ liệu cục bộ
                documentVersionDao.insertVersion(localVersion.toEntity())

                // Cập nhật phiên bản mới nhất của tài liệu
                documentDao.updateLatestVersion(version.documentId, localId)

                // Lưu file vào bộ nhớ cục bộ
                val savedFile = saveFileLocally(file, "${version.documentId}_v${nextVersionNumber}")

                // Đồng bộ với API nếu có kết nối mạng
                if (networkUtils.isNetworkAvailable()) {
                    try {
                        val request = CreateDocumentVersionRequest(
                            changeNotes = localVersion.changeNotes
                        )

                        val requestBody = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
                        val filePart = MultipartBody.Part.createFormData("file", file.name, requestBody)

                        val response = documentApiService.createVersion(
                            documentId = version.documentId,
                            request = request,
                            file = filePart
                        )

                        // Cập nhật thông tin từ server
                        val serverVersion = response.data.toDomain()
                        val updatedVersion = localVersion.copy(
                            serverId = serverVersion.id,
                            syncStatus = "synced"
                        )

                        documentVersionDao.updateVersion(updatedVersion.toEntity())
                        return@withContext Resource.Success(updatedVersion)
                    } catch (e: Exception) {
                        // Giữ phiên bản cục bộ, sẽ đồng bộ sau
                        return@withContext Resource.Success(localVersion)
                    }
                } else {
                    // Không có kết nối mạng, giữ phiên bản cục bộ
                    return@withContext Resource.Success(localVersion)
                }
            } catch (e: Exception) {
                return@withContext Resource.Error("Không thể tạo phiên bản mới: ${e.message}")
            }
        }
    }

    // Document Permissions
    override fun getPermissionsByDocument(documentId: String): Flow<Resource<List<DocumentPermission>>> {
        return flow {
            emit(Resource.Loading)
            try {
                documentPermissionDao.getPermissionsByDocument(documentId).collect { localPermissions ->
                    emit(Resource.Success(localPermissions.map { it.toDomain() }))
                }
            } catch (e: Exception) {
                emit(Resource.Error("Không thể tải danh sách quyền: ${e.message}"))
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getUserPermission(documentId: String, userId: String): Resource<DocumentPermission> {
        return Resource.Error("Chức năng chưa được triển khai")
    }

    override suspend fun createPermission(permission: DocumentPermission): Resource<DocumentPermission> {
        return Resource.Error("Chức năng chưa được triển khai")
    }

    override suspend fun deletePermission(documentId: String, userId: String): Resource<Unit> {
        return Resource.Error("Chức năng chưa được triển khai")
    }

    // Synchronization
    override suspend fun syncDocuments(): Resource<Unit> {
        return Resource.Error("Chức năng chưa được triển khai")
    }

    // Helper methods
    private fun saveFileLocally(file: File, documentId: String): File {
        val documentsDir = File(context.filesDir, "documents")
        if (!documentsDir.exists()) {
            documentsDir.mkdirs()
        }

        val destinationFile = File(documentsDir, documentId)
        try {
            file.copyTo(destinationFile, overwrite = true)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return destinationFile
    }
}
