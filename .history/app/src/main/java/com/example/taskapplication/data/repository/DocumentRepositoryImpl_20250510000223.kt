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
    
    // More implementation methods will be added in the next part
}
