package com.example.taskapplication.data.repository

import android.content.Context
import com.example.taskapplication.data.database.dao.DocumentDao
import com.example.taskapplication.data.database.dao.DocumentFolderDao
import com.example.taskapplication.data.database.dao.DocumentPermissionDao
import com.example.taskapplication.data.database.dao.DocumentVersionDao
import com.example.taskapplication.data.mapper.toDomain
import com.example.taskapplication.data.mapper.toEntity
import com.example.taskapplication.data.remote.api.DocumentApiService
import com.example.taskapplication.data.remote.dto.toDomain
import com.example.taskapplication.data.remote.request.CreateDocumentFolderRequest
import com.example.taskapplication.data.remote.request.CreateDocumentPermissionRequest
import com.example.taskapplication.data.remote.request.CreateDocumentRequest
import com.example.taskapplication.data.remote.request.CreateDocumentVersionRequest
import com.example.taskapplication.data.remote.request.UpdateDocumentFolderRequest
import com.example.taskapplication.data.remote.request.UpdateDocumentRequest
import com.example.taskapplication.domain.model.Document
import com.example.taskapplication.domain.model.DocumentFolder
import com.example.taskapplication.domain.model.DocumentPermission
import com.example.taskapplication.domain.model.DocumentVersion
import com.example.taskapplication.domain.repository.DocumentRepository
import com.example.taskapplication.util.Resource
import com.example.taskapplication.util.UuidGenerator
import com.example.taskapplication.util.safeApiCall
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.Date
import java.util.UUID
import javax.inject.Inject

/**
 * Implementation of DocumentRepository
 */
class DocumentRepositoryImpl @Inject constructor(
    private val documentApiService: DocumentApiService,
    private val documentFolderDao: DocumentFolderDao,
    private val documentDao: DocumentDao,
    private val documentVersionDao: DocumentVersionDao,
    private val documentPermissionDao: DocumentPermissionDao,
    @ApplicationContext private val context: Context
) : DocumentRepository {

    // Document Folders

    override fun getFoldersByTeam(teamId: String): Flow<Resource<List<DocumentFolder>>> {
        return documentFolderDao.getFoldersByTeam(teamId).map { entities ->
            Resource.Success(entities.map { it.toDomain() })
        }
    }

    override fun getSubfolders(folderId: String): Flow<Resource<List<DocumentFolder>>> {
        return documentFolderDao.getSubfolders(folderId).map { entities ->
            Resource.Success(entities.map { it.toDomain() })
        }
    }

    override fun getRootFolders(teamId: String): Flow<Resource<List<DocumentFolder>>> {
        return documentFolderDao.getRootFolders(teamId).map { entities ->
            Resource.Success(entities.map { it.toDomain() })
        }
    }

    override suspend fun getFolderById(folderId: String): Resource<DocumentFolder> {
        val localFolder = documentFolderDao.getFolderById(folderId)
        return if (localFolder != null) {
            Resource.Success(localFolder.toDomain())
        } else {
            // Try to fetch from API
            val result = safeApiCall { documentApiService.getFolderById(folderId) }
            when (result) {
                is Resource.Success -> {
                    val folder = result.data.data.toDomain()
                    documentFolderDao.insertFolder(folder.toEntity())
                    Resource.Success(folder)
                }
                is Resource.Error -> result
                is Resource.Loading -> Resource.Loading()
            }
        }
    }

    override suspend fun createFolder(folder: DocumentFolder): Resource<DocumentFolder> {
        // Create a local folder first
        val newFolder = folder.copy(
            id = folder.id.ifEmpty { UuidGenerator.generateUuid() },
            createdAt = Date(),
            updatedAt = Date(),
            syncStatus = "pending"
        )
        documentFolderDao.insertFolder(newFolder.toEntity())

        // Try to sync with API
        val request = CreateDocumentFolderRequest(
            name = newFolder.name,
            description = newFolder.description,
            teamId = newFolder.teamId,
            parentFolderId = newFolder.parentFolderId
        )

        val result = safeApiCall { documentApiService.createFolder(request) }
        return when (result) {
            is Resource.Success -> {
                val serverFolder = result.data.data.toDomain()
                val updatedFolder = newFolder.copy(
                    serverId = serverFolder.id,
                    syncStatus = "synced"
                )
                documentFolderDao.updateFolder(updatedFolder.toEntity())
                Resource.Success(updatedFolder)
            }
            is Resource.Error -> {
                // Keep the local folder, will sync later
                Resource.Success(newFolder)
            }
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun updateFolder(folder: DocumentFolder): Resource<DocumentFolder> {
        // Update local folder first
        val updatedFolder = folder.copy(
            updatedAt = Date(),
            syncStatus = "pending"
        )
        documentFolderDao.updateFolder(updatedFolder.toEntity())

        // Try to sync with API if we have a server ID
        if (folder.serverId != null) {
            val request = UpdateDocumentFolderRequest(
                name = updatedFolder.name,
                description = updatedFolder.description,
                parentFolderId = updatedFolder.parentFolderId
            )

            val result = safeApiCall { documentApiService.updateFolder(folder.serverId, request) }
            return when (result) {
                is Resource.Success -> {
                    val serverFolder = result.data.data.toDomain()
                    val finalFolder = updatedFolder.copy(
                        syncStatus = "synced"
                    )
                    documentFolderDao.updateFolder(finalFolder.toEntity())
                    Resource.Success(finalFolder)
                }
                is Resource.Error -> {
                    // Keep the local folder, will sync later
                    Resource.Success(updatedFolder)
                }
                is Resource.Loading -> Resource.Loading()
            }
        }

        return Resource.Success(updatedFolder)
    }

    override suspend fun deleteFolder(folderId: String): Resource<Unit> {
        // Mark local folder as deleted
        documentFolderDao.markFolderAsDeleted(folderId)

        // Try to sync with API
        val folder = documentFolderDao.getFolderById(folderId)
        if (folder?.serverId != null) {
            val result = safeApiCall { documentApiService.deleteFolder(folder.serverId) }
            return when (result) {
                is Resource.Success -> Resource.Success(Unit)
                is Resource.Error -> Resource.Success(Unit) // Still return success since we marked it locally
                is Resource.Loading -> Resource.Loading()
            }
        }

        return Resource.Success(Unit)
    }

    // Documents

    override fun getDocumentsByTeam(teamId: String): Flow<Resource<List<Document>>> {
        return documentDao.getDocumentsByTeam(teamId).map { entities ->
            Resource.Success(entities.map { it.toDomain() })
        }
    }

    override fun getDocumentsByFolder(folderId: String): Flow<Resource<List<Document>>> {
        return documentDao.getDocumentsByFolder(folderId).map { entities ->
            Resource.Success(entities.map { it.toDomain() })
        }
    }

    override fun getRootDocuments(teamId: String): Flow<Resource<List<Document>>> {
        return documentDao.getRootDocuments(teamId).map { entities ->
            Resource.Success(entities.map { it.toDomain() })
        }
    }

    override suspend fun getDocumentById(documentId: String): Resource<Document> {
        val localDocument = documentDao.getDocumentById(documentId)
        return if (localDocument != null) {
            Resource.Success(localDocument.toDomain())
        } else {
            // Try to fetch from API
            val result = safeApiCall { documentApiService.getDocumentById(documentId) }
            when (result) {
                is Resource.Success -> {
                    val document = result.data.data.toDomain()
                    documentDao.insertDocument(document.toEntity())
                    Resource.Success(document)
                }
                is Resource.Error -> result
                is Resource.Loading -> Resource.Loading()
            }
        }
    }

    override fun getDocumentByIdFlow(documentId: String): Flow<Resource<Document>> {
        return documentDao.getDocumentByIdFlow(documentId).map { entity ->
            if (entity != null) {
                Resource.Success(entity.toDomain())
            } else {
                Resource.Error("Document not found")
            }
        }
    }

    override suspend fun createDocument(document: Document, file: File): Resource<Document> {
        // Create a local document first
        val newDocument = document.copy(
            id = document.id.ifEmpty { UuidGenerator.generateUuid() },
            uploadedAt = Date(),
            lastModified = Date(),
            syncStatus = "pending"
        )
        documentDao.insertDocument(newDocument.toEntity())

        // Try to sync with API
        val request = CreateDocumentRequest(
            name = newDocument.name,
            description = newDocument.description,
            teamId = newDocument.teamId,
            folderId = newDocument.folderId,
            accessLevel = newDocument.accessLevel,
            allowedUsers = newDocument.allowedUsers
        )

        val requestBody = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("file", file.name, requestBody)

        val result = safeApiCall { documentApiService.createDocument(request, filePart) }
        return when (result) {
            is Resource.Success -> {
                val serverDocument = result.data.data.toDomain()
                val updatedDocument = newDocument.copy(
                    serverId = serverDocument.id,
                    syncStatus = "synced"
                )
                documentDao.updateDocument(updatedDocument.toEntity())
                Resource.Success(updatedDocument)
            }
            is Resource.Error -> {
                // Keep the local document, will sync later
                Resource.Success(newDocument)
            }
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun updateDocument(document: Document): Resource<Document> {
        // Update local document first
        val updatedDocument = document.copy(
            lastModified = Date(),
            syncStatus = "pending"
        )
        documentDao.updateDocument(updatedDocument.toEntity())

        // Try to sync with API if we have a server ID
        if (document.serverId != null) {
            val request = UpdateDocumentRequest(
                name = updatedDocument.name,
                description = updatedDocument.description,
                folderId = updatedDocument.folderId,
                accessLevel = updatedDocument.accessLevel,
                allowedUsers = updatedDocument.allowedUsers
            )

            val result = safeApiCall { documentApiService.updateDocument(document.serverId, request) }
            return when (result) {
                is Resource.Success -> {
                    val serverDocument = result.data.data.toDomain()
                    val finalDocument = updatedDocument.copy(
                        syncStatus = "synced"
                    )
                    documentDao.updateDocument(finalDocument.toEntity())
                    Resource.Success(finalDocument)
                }
                is Resource.Error -> {
                    // Keep the local document, will sync later
                    Resource.Success(updatedDocument)
                }
                is Resource.Loading -> Resource.Loading()
            }
        }

        return Resource.Success(updatedDocument)
    }

    override suspend fun deleteDocument(documentId: String): Resource<Unit> {
        // Mark local document as deleted
        documentDao.markDocumentAsDeleted(documentId)

        // Try to sync with API
        val document = documentDao.getDocumentById(documentId)
        if (document?.serverId != null) {
            val result = safeApiCall { documentApiService.deleteDocument(document.serverId) }
            return when (result) {
                is Resource.Success -> Resource.Success(Unit)
                is Resource.Error -> Resource.Success(Unit) // Still return success since we marked it locally
                is Resource.Loading -> Resource.Loading()
            }
        }

        return Resource.Success(Unit)
    }

    override suspend fun downloadDocument(documentId: String, versionId: String?): Resource<File> {
        // Try to download from API
        val result = safeApiCall { documentApiService.downloadDocument(documentId, versionId) }
        return when (result) {
            is Resource.Success -> {
                try {
                    val responseBody = result.data.body() ?: return Resource.Error("Empty response")

                    // Create a temporary file
                    val fileName = "document_${documentId}_${versionId ?: "latest"}.tmp"
                    val file = File(context.cacheDir, fileName)

                    // Write the file
                    file.outputStream().use { fileOutputStream ->
                        responseBody.byteStream().use { inputStream ->
                            inputStream.copyTo(fileOutputStream)
                        }
                    }

                    Resource.Success(file)
                } catch (e: Exception) {
                    Resource.Error("Failed to download file: ${e.message}")
                }
            }
            is Resource.Error -> result
            is Resource.Loading -> Resource.Loading()
        }
    }

    override fun searchDocuments(teamId: String, query: String): Flow<Resource<List<Document>>> {
        return documentDao.searchDocuments(teamId, query).map { entities ->
            Resource.Success(entities.map { it.toDomain() })
        }
    }

    // Document Versions

    override fun getVersionsByDocument(documentId: String): Flow<Resource<List<DocumentVersion>>> {
        return documentVersionDao.getVersionsByDocument(documentId).map { entities ->
            Resource.Success(entities.map { it.toDomain() })
        }
    }

    override suspend fun getVersionById(versionId: String): Resource<DocumentVersion> {
        val localVersion = documentVersionDao.getVersionById(versionId)
        return if (localVersion != null) {
            Resource.Success(localVersion.toDomain())
        } else {
            Resource.Error("Version not found")
        }
    }

    override suspend fun getLatestVersion(documentId: String): Resource<DocumentVersion> {
        val latestVersion = documentVersionDao.getLatestVersion(documentId)
        return if (latestVersion != null) {
            Resource.Success(latestVersion.toDomain())
        } else {
            Resource.Error("No versions found for document")
        }
    }

    override suspend fun createVersion(version: DocumentVersion, file: File): Resource<DocumentVersion> {
        // Get next version number
        val nextVersionNumber = documentVersionDao.getNextVersionNumber(version.documentId)

        // Create a local version first
        val newVersion = version.copy(
            id = version.id.ifEmpty { UuidGenerator.generateUuid() },
            versionNumber = nextVersionNumber,
            uploadedAt = Date(),
            syncStatus = "pending"
        )
        documentVersionDao.insertVersion(newVersion.toEntity())

        // Update document's latest version
        documentDao.updateDocumentLatestVersion(version.documentId, newVersion.id)

        // Try to sync with API
        val request = CreateDocumentVersionRequest(
            changeNotes = newVersion.changeNotes
        )

        val requestBody = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("file", file.name, requestBody)

        val result = safeApiCall {
            documentApiService.createVersion(version.documentId, request, filePart)
        }
        return when (result) {
            is Resource.Success -> {
                val serverVersion = result.data.data.toDomain()
                val updatedVersion = newVersion.copy(
                    serverId = serverVersion.id,
                    syncStatus = "synced"
                )
                documentVersionDao.insertVersion(updatedVersion.toEntity())
                Resource.Success(updatedVersion)
            }
            is Resource.Error -> {
                // Keep the local version, will sync later
                Resource.Success(newVersion)
            }
            is Resource.Loading -> Resource.Loading()
        }
    }

    // Document Permissions

    override fun getPermissionsByDocument(documentId: String): Flow<Resource<List<DocumentPermission>>> {
        return documentPermissionDao.getPermissionsByDocument(documentId).map { entities ->
            Resource.Success(entities.map { it.toDomain() })
        }
    }

    override suspend fun getUserPermission(documentId: String, userId: String): Resource<DocumentPermission> {
        val permission = documentPermissionDao.getUserPermission(documentId, userId)
        return if (permission != null) {
            Resource.Success(permission.toDomain())
        } else {
            Resource.Error("Permission not found")
        }
    }

    override suspend fun createPermission(permission: DocumentPermission): Resource<DocumentPermission> {
        // Create a local permission first
        val newPermission = permission.copy(
            id = permission.id.ifEmpty { UuidGenerator.generateUuid() },
            grantedAt = Date(),
            syncStatus = "pending"
        )
        documentPermissionDao.insertPermission(newPermission.toEntity())

        // Try to sync with API
        val request = CreateDocumentPermissionRequest(
            userId = newPermission.userId,
            permissionType = newPermission.permissionType
        )

        val result = safeApiCall {
            documentApiService.createPermission(permission.documentId, request)
        }
        return when (result) {
            is Resource.Success -> {
                val serverPermission = result.data.data.toDomain()
                val updatedPermission = newPermission.copy(
                    serverId = serverPermission.id,
                    syncStatus = "synced"
                )
                documentPermissionDao.insertPermission(updatedPermission.toEntity())
                Resource.Success(updatedPermission)
            }
            is Resource.Error -> {
                // Keep the local permission, will sync later
                Resource.Success(newPermission)
            }
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun deletePermission(documentId: String, userId: String): Resource<Unit> {
        // Delete local permission
        documentPermissionDao.deletePermission(documentId, userId)

        // Try to sync with API
        val result = safeApiCall { documentApiService.deletePermission(documentId, userId) }
        return when (result) {
            is Resource.Success -> Resource.Success(Unit)
            is Resource.Error -> Resource.Success(Unit) // Still return success since we deleted it locally
            is Resource.Loading -> Resource.Loading()
        }
    }

    // Sync

    override suspend fun syncDocuments(): Resource<Unit> {
        // Get all local changes that need to be synced
        val unsyncedFolders = documentFolderDao.getUnSyncedFolders()
        val unsyncedDocuments = documentDao.getUnSyncedDocuments()
        val unsyncedVersions = documentVersionDao.getUnSyncedVersions()
        val unsyncedPermissions = documentPermissionDao.getUnSyncedPermissions()

        // Prepare sync data
        val syncData = mapOf(
            "folders" to unsyncedFolders.map {
                mapOf(
                    "id" to it.id,
                    "name" to it.name,
                    "description" to it.description,
                    "team_id" to it.teamId,
                    "parent_folder_id" to it.parentFolderId,
                    "is_deleted" to it.isDeleted
                )
            },
            "documents" to unsyncedDocuments.map {
                mapOf(
                    "id" to it.id,
                    "name" to it.name,
                    "description" to it.description,
                    "team_id" to it.teamId,
                    "folder_id" to it.folderId,
                    "access_level" to it.accessLevel,
                    "allowed_users" to it.allowedUsers.split(",").filter { it.isNotBlank() },
                    "is_deleted" to it.isDeleted
                )
            },
            "versions" to unsyncedVersions.map {
                mapOf(
                    "id" to it.id,
                    "document_id" to it.documentId,
                    "version_number" to it.versionNumber,
                    "change_notes" to it.changeNotes
                )
            },
            "permissions" to unsyncedPermissions.map {
                mapOf(
                    "id" to it.id,
                    "document_id" to it.documentId,
                    "user_id" to it.userId,
                    "permission_type" to it.permissionType
                )
            }
        )

        // Send sync data to server
        val result = safeApiCall { documentApiService.syncDocuments(syncData) }
        return when (result) {
            is Resource.Success -> {
                val syncResponse = result.data.data

                // Update local data with server changes

                // Folders
                syncResponse.folders.created.forEach { folderDto ->
                    val folder = folderDto.toDomain()
                    documentFolderDao.insertFolder(folder.toEntity())
                }

                syncResponse.folders.updated.forEach { folderDto ->
                    val folder = folderDto.toDomain()
                    documentFolderDao.insertFolder(folder.toEntity())
                }

                syncResponse.folders.serverDeletedIds.forEach { folderId ->
                    documentFolderDao.markFolderAsDeleted(folderId)
                }

                // Documents
                syncResponse.documents.created.forEach { documentDto ->
                    val document = documentDto.toDomain()
                    documentDao.insertDocument(document.toEntity())
                }

                syncResponse.documents.updated.forEach { documentDto ->
                    val document = documentDto.toDomain()
                    documentDao.insertDocument(document.toEntity())
                }

                syncResponse.documents.serverDeletedIds.forEach { documentId ->
                    documentDao.markDocumentAsDeleted(documentId)
                }

                // Versions
                syncResponse.versions.created.forEach { versionDto ->
                    val version = versionDto.toDomain()
                    documentVersionDao.insertVersion(version.toEntity())
                }

                // Permissions
                syncResponse.permissions.created.forEach { permissionDto ->
                    val permission = permissionDto.toDomain()
                    documentPermissionDao.insertPermission(permission.toEntity())
                }

                // Update sync status for local changes
                unsyncedFolders.forEach { folder ->
                    documentFolderDao.updateFolderSyncStatus(folder.id, "synced", folder.id)
                }

                unsyncedDocuments.forEach { document ->
                    documentDao.updateDocumentSyncStatus(document.id, "synced", document.id)
                }

                unsyncedVersions.forEach { version ->
                    documentVersionDao.updateVersionSyncStatus(version.id, "synced", version.id)
                }

                unsyncedPermissions.forEach { permission ->
                    documentPermissionDao.updatePermissionSyncStatus(permission.id, "synced", permission.id)
                }

                Resource.Success(Unit)
            }
            is Resource.Error -> result
            is Resource.Loading -> Resource.Loading()
        }
    }
}
