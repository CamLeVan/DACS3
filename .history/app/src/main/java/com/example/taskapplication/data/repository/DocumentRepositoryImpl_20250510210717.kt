package com.example.taskapplication.data.repository

import android.content.Context
import com.example.taskapplication.data.database.dao.DocumentDao
import com.example.taskapplication.data.database.dao.DocumentFolderDao
import com.example.taskapplication.data.database.dao.DocumentPermissionDao
import com.example.taskapplication.data.database.dao.DocumentVersionDao
import com.example.taskapplication.data.mapper.toDomain
import com.example.taskapplication.data.mapper.toEntity
import com.example.taskapplication.data.remote.ApiService
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
        return Resource.Error("Chức năng chưa được triển khai")
    }

    override fun searchDocuments(teamId: String, query: String): Flow<Resource<List<Document>>> {
        return flow {
            emit(Resource.Loading)
            try {
                val localResults = documentDao.searchDocuments(teamId, "%$query%")
                emit(Resource.Success(localResults.map { it.toDomain() }))
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
                val localVersions = documentVersionDao.getVersionsByDocument(documentId)
                emit(Resource.Success(localVersions.map { it.toDomain() }))
            } catch (e: Exception) {
                emit(Resource.Error("Không thể tải danh sách phiên bản: ${e.message}"))
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getVersionById(versionId: String): Resource<DocumentVersion> {
        return Resource.Error("Chức năng chưa được triển khai")
    }

    override suspend fun getLatestVersion(documentId: String): Resource<DocumentVersion> {
        return Resource.Error("Chức năng chưa được triển khai")
    }

    override suspend fun createVersion(version: DocumentVersion, file: File): Resource<DocumentVersion> {
        return Resource.Error("Chức năng chưa được triển khai")
    }

    // Document Permissions
    override fun getPermissionsByDocument(documentId: String): Flow<Resource<List<DocumentPermission>>> {
        return flow {
            emit(Resource.Loading)
            try {
                val localPermissions = documentPermissionDao.getPermissionsByDocument(documentId)
                emit(Resource.Success(localPermissions.map { it.toDomain() }))
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
