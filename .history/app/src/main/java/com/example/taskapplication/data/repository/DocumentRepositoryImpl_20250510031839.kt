package com.example.taskapplication.data.repository

import android.content.Context
import com.example.taskapplication.data.database.dao.DocumentDao
import com.example.taskapplication.data.database.dao.DocumentFolderDao
import com.example.taskapplication.data.database.dao.DocumentPermissionDao
import com.example.taskapplication.data.database.dao.DocumentVersionDao
import com.example.taskapplication.domain.model.Document
import com.example.taskapplication.domain.model.DocumentFolder
import com.example.taskapplication.domain.model.DocumentPermission
import com.example.taskapplication.domain.model.DocumentVersion
import com.example.taskapplication.domain.repository.DocumentRepository
import com.example.taskapplication.util.Resource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of DocumentRepository
 *
 * Note: This is a temporary implementation to fix build errors
 */
@Singleton
class DocumentRepositoryImpl @Inject constructor(
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
        val entity = documentFolderDao.getFolderById(folderId) ?: return Resource.Error("Folder not found")
        return Resource.Success(entity.toDomain())
    }

    override suspend fun createFolder(folder: DocumentFolder): Resource<DocumentFolder> {
        val entity = folder.toEntity(UUID.randomUUID().toString())
        documentFolderDao.insertFolder(entity)
        return Resource.Success(entity.toDomain())
    }

    override suspend fun updateFolder(folder: DocumentFolder): Resource<DocumentFolder> {
        val existingFolder = documentFolderDao.getFolderById(folder.id)
            ?: return Resource.Error("Folder not found")

        val updatedEntity = existingFolder.copy(
            name = folder.name,
            description = folder.description,
            parentFolderId = folder.parentFolderId,
            updatedAt = System.currentTimeMillis(),
            syncStatus = "pending_update"
        )

        documentFolderDao.updateFolder(updatedEntity)
        return Resource.Success(updatedEntity.toDomain())
    }

    override suspend fun deleteFolder(folderId: String): Resource<Unit> {
        val folder = documentFolderDao.getFolderById(folderId)
            ?: return Resource.Error("Folder not found")

        val updatedEntity = folder.copy(
            isDeleted = true,
            updatedAt = System.currentTimeMillis(),
            syncStatus = "pending_delete"
        )

        documentFolderDao.updateFolder(updatedEntity)
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
        val entity = documentDao.getDocumentById(documentId) ?: return Resource.Error("Document not found")
        return Resource.Success(entity.toDomain())
    }

    override suspend fun createDocument(document: Document, file: File): Resource<Document> {
        // Tạo ID mới cho tài liệu
        val documentId = UUID.randomUUID().toString()

        // Lưu file vào bộ nhớ cục bộ
        val savedFile = saveFileLocally(file, documentId)

        // Tạo entity và lưu vào cơ sở dữ liệu
        val entity = document.toEntity(
            id = documentId,
            fileUrl = savedFile.absolutePath,
            createdAt = System.currentTimeMillis(),
            lastModified = System.currentTimeMillis()
        )

        documentDao.insertDocument(entity)
        return Resource.Success(entity.toDomain())
    }

    override suspend fun updateDocument(document: Document): Resource<Document> {
        val existingDocument = documentDao.getDocumentById(document.id)
            ?: return Resource.Error("Document not found")

        val updatedEntity = existingDocument.copy(
            name = document.name,
            description = document.description,
            folderId = document.folderId,
            accessLevel = document.accessLevel,
            allowedUsers = document.allowedUsers.joinToString(","),
            lastModified = System.currentTimeMillis(),
            syncStatus = "pending_update"
        )

        documentDao.updateDocument(updatedEntity)
        return Resource.Success(updatedEntity.toDomain())
    }

    override suspend fun deleteDocument(documentId: String): Resource<Unit> {
        val document = documentDao.getDocumentById(documentId)
            ?: return Resource.Error("Document not found")

        val updatedEntity = document.copy(
            isDeleted = true,
            lastModified = System.currentTimeMillis(),
            syncStatus = "pending_delete"
        )

        documentDao.updateDocument(updatedEntity)
        return Resource.Success(Unit)
    }

    // Document Versions

    override suspend fun getDocumentVersions(documentId: String): Resource<List<DocumentVersion>> {
        val versions = documentVersionDao.getVersionsByDocument(documentId)
        return Resource.Success(versions.map { it.toDomain() })
    }

    override suspend fun createDocumentVersion(version: DocumentVersion, file: File): Resource<DocumentVersion> {
        // Tạo ID mới cho phiên bản
        val versionId = UUID.randomUUID().toString()

        // Lưu file vào bộ nhớ cục bộ
        val savedFile = saveFileLocally(file, "${version.documentId}_v${version.versionNumber}")

        // Tạo entity và lưu vào cơ sở dữ liệu
        val entity = version.toEntity(
            id = versionId,
            fileUrl = savedFile.absolutePath,
            uploadedAt = System.currentTimeMillis()
        )

        documentVersionDao.insertVersion(entity)

        // Cập nhật latestVersionId trong document
        val document = documentDao.getDocumentById(version.documentId)
        if (document != null) {
            documentDao.updateDocument(document.copy(
                latestVersionId = versionId,
                lastModified = System.currentTimeMillis(),
                syncStatus = "pending_update"
            ))
        }

        return Resource.Success(entity.toDomain())
    }

    // Document Permissions

    override suspend fun getDocumentPermissions(documentId: String): Resource<List<DocumentPermission>> {
        val permissions = documentPermissionDao.getPermissionsByDocument(documentId)
        return Resource.Success(permissions.map { it.toDomain() })
    }

    override suspend fun createDocumentPermission(permission: DocumentPermission): Resource<DocumentPermission> {
        val entity = permission.toEntity(UUID.randomUUID().toString(), System.currentTimeMillis())
        documentPermissionDao.insertPermission(entity)
        return Resource.Success(entity.toDomain())
    }

    override suspend fun deleteDocumentPermission(permissionId: String): Resource<Unit> {
        val permission = documentPermissionDao.getPermissionById(permissionId)
            ?: return Resource.Error("Permission not found")

        documentPermissionDao.deletePermission(permission)
        return Resource.Success(Unit)
    }

    // Synchronization

    override suspend fun syncDocuments(): Resource<Unit> {
        // Đây là phương thức giả định để đồng bộ hóa tài liệu
        // Trong triển khai thực tế, bạn sẽ gửi các thay đổi cục bộ lên máy chủ
        // và nhận các thay đổi từ máy chủ
        return Resource.Success(Unit)
    }

    // Helper methods

    private fun saveFileLocally(file: File, name: String): File {
        val documentsDir = File(context.filesDir, "documents")
        if (!documentsDir.exists()) {
            documentsDir.mkdirs()
        }

        val destinationFile = File(documentsDir, name)
        file.copyTo(destinationFile, overwrite = true)
        return destinationFile
    }
}
