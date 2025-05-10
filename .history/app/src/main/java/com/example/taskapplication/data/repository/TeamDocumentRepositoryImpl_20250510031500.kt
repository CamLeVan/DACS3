package com.example.taskapplication.data.repository

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.database.dao.TeamDocumentDao
import com.example.taskapplication.data.database.dao.UserDao
import com.example.taskapplication.data.mapper.toDomainModel
import com.example.taskapplication.data.mapper.toEntity
import com.example.taskapplication.util.ConnectionChecker
import com.example.taskapplication.util.DataStoreManager
import com.example.taskapplication.util.NetworkUtils
import com.example.taskapplication.domain.model.TeamDocument
import com.example.taskapplication.domain.repository.TeamDocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeamDocumentRepositoryImpl @Inject constructor(
    private val teamDocumentDao: TeamDocumentDao,
    private val userDao: UserDao,
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager,
    private val connectionChecker: ConnectionChecker,
    private val networkUtils: NetworkUtils,
    private val context: Context
) : TeamDocumentRepository {

    private val TAG = "TeamDocumentRepository"

    override fun getDocumentsByTeam(teamId: String): Flow<List<TeamDocument>> {
        return teamDocumentDao.getDocumentsByTeam(teamId.toLongOrNull() ?: 0L)
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                // Log error and emit empty list
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getDocumentById(documentId: String): TeamDocument? {
        return withContext(Dispatchers.IO) {
            teamDocumentDao.getDocumentById(documentId.toLongOrNull() ?: 0L)?.toDomainModel()
        }
    }

    override fun getDocumentsByUser(userId: String): Flow<List<TeamDocument>> {
        return teamDocumentDao.getDocumentsByUser(userId.toLongOrNull() ?: 0L)
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                // Log error and emit empty list
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)
    }

    override fun getDocumentsByAccessLevel(teamId: String, accessLevel: String): Flow<List<TeamDocument>> {
        return teamDocumentDao.getDocumentsByAccessLevel(teamId.toLongOrNull() ?: 0L, accessLevel)
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                // Log error and emit empty list
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)
    }

    override fun getDocumentsByFileType(teamId: String, fileType: String): Flow<List<TeamDocument>> {
        return teamDocumentDao.getDocumentsByFileType(teamId.toLongOrNull() ?: 0L, fileType)
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                // Log error and emit empty list
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)
    }

    override fun searchDocuments(teamId: String, query: String): Flow<List<TeamDocument>> {
        return teamDocumentDao.searchDocuments(teamId.toLongOrNull() ?: 0L, query)
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                // Log error and emit empty list
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun uploadDocument(
        teamId: String,
        name: String,
        description: String,
        file: File,
        accessLevel: String,
        allowedUsers: List<String>
    ): Result<TeamDocument> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = "current_user" // Temporary placeholder

                // Get file type
                val fileExtension = file.extension
                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension) ?: "application/octet-stream"
                val fileType = when {
                    mimeType.startsWith("image/") -> "image"
                    mimeType.startsWith("video/") -> "video"
                    mimeType.startsWith("audio/") -> "audio"
                    mimeType == "application/pdf" -> "pdf"
                    mimeType.contains("word") -> "doc"
                    mimeType.contains("excel") || mimeType.contains("sheet") -> "spreadsheet"
                    mimeType.contains("powerpoint") || mimeType.contains("presentation") -> "presentation"
                    else -> "other"
                }

                // Create document entity
                val documentId = UUID.randomUUID().toString()
                val timestamp = System.currentTimeMillis()
                val document = TeamDocument(
                    id = documentId,
                    teamId = teamId,
                    name = name,
                    description = description,
                    fileUrl = file.absolutePath, // Temporary local path
                    thumbnailUrl = null,
                    fileType = fileType,
                    fileSize = file.length(),
                    folderId = null,
                    uploadedBy = userId,
                    uploadedAt = timestamp,
                    lastModified = timestamp,
                    syncStatus = "pending",
                    accessLevel = accessLevel,
                    allowedUsers = allowedUsers
                )

                // Save to local database
                teamDocumentDao.insertDocument(document.toEntity())

                // If connected, upload to server
                if (networkUtils.isNetworkAvailable()) {
                    try {
                        // TODO: Implement file upload to server
                        // For now, just update sync status
                        teamDocumentDao.updateDocumentSyncStatus(documentId.toLongOrNull() ?: 0L, "synced", "server_id_placeholder".toLongOrNull())
                    } catch (e: Exception) {
                        // Log error but don't fail the operation
                    }
                }

                Result.success(document)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateDocument(document: TeamDocument): Result<TeamDocument> {
        return withContext(Dispatchers.IO) {
            try {
                val updatedDocument = document.copy(
                    lastModified = System.currentTimeMillis(),
                    syncStatus = "pending"
                )
                teamDocumentDao.updateDocument(updatedDocument.toEntity())

                // If connected, sync with server
                if (networkUtils.isNetworkAvailable()) {
                    try {
                        syncDocuments()
                    } catch (e: Exception) {
                        // Log error but don't fail the operation
                    }
                }

                Result.success(updatedDocument)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteDocument(documentId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                teamDocumentDao.markDocumentAsDeleted(documentId.toLongOrNull() ?: 0L)

                // If connected, sync with server
                if (networkUtils.isNetworkAvailable()) {
                    try {
                        syncDocuments()
                    } catch (e: Exception) {
                        // Log error but don't fail the operation
                    }
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun downloadDocument(documentId: String): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                val document = teamDocumentDao.getDocumentById(documentId.toLongOrNull() ?: 0L) ?: return@withContext Result.failure(
                    IllegalArgumentException("Document not found")
                )

                // If the file is already local, just return it
                val localFile = File(document.fileUrl)
                if (localFile.exists()) {
                    return@withContext Result.success(localFile)
                }

                // If the file is on the server, download it
                if (document.serverId != null && networkUtils.isNetworkAvailable()) {
                    // TODO: Implement file download from server
                    // For now, just return a failure
                    return@withContext Result.failure(
                        NotImplementedError("File download not implemented yet")
                    )
                }

                Result.failure(IOException("File not found"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateDocumentAccessLevel(
        documentId: String,
        accessLevel: String,
        allowedUsers: List<String>
    ): Result<TeamDocument> {
        return withContext(Dispatchers.IO) {
            try {
                val docId = documentId.toLongOrNull() ?: 0L
                val document = teamDocumentDao.getDocumentById(docId) ?: return@withContext Result.failure(
                    IllegalArgumentException("Document not found")
                )

                teamDocumentDao.updateDocumentAccessLevel(
                    docId,
                    accessLevel,
                    allowedUsers.joinToString(",")
                )

                val updatedDocument = teamDocumentDao.getDocumentById(documentId)?.toDomainModel()
                    ?: return@withContext Result.failure(IllegalStateException("Failed to update document"))

                // If connected, sync with server
                if (networkUtils.isNetworkAvailable()) {
                    try {
                        syncDocuments()
                    } catch (e: Exception) {
                        // Log error but don't fail the operation
                    }
                }

                Result.success(updatedDocument)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun syncDocuments(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (!networkUtils.isNetworkAvailable()) {
                    return@withContext Result.failure(IOException("No network connection"))
                }

                // Get documents that need to be synced
                val unsyncedDocuments = teamDocumentDao.getUnSyncedDocuments()

                // TODO: Implement sync with server
                // For now, just mark all as synced
                unsyncedDocuments.forEach { document ->
                    teamDocumentDao.updateDocumentSyncStatus(
                        document.id,
                        "synced",
                        document.serverId ?: "server_id_placeholder"
                    )
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
