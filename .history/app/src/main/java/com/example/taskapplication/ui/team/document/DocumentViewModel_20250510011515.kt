package com.example.taskapplication.ui.team.document

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskapplication.domain.model.Document
import com.example.taskapplication.domain.model.DocumentFolder
import com.example.taskapplication.domain.model.DocumentPermission
import com.example.taskapplication.domain.model.DocumentVersion
import com.example.taskapplication.domain.repository.DocumentRepository
import com.example.taskapplication.domain.repository.UserRepository
import com.example.taskapplication.util.Resource
import com.example.taskapplication.util.UuidGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel for document operations
 */
@HiltViewModel
class DocumentViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // State for document list
    private val _documentListState = MutableStateFlow(DocumentListState())
    val documentListState: StateFlow<DocumentListState> = _documentListState.asStateFlow()

    // State for document detail
    private val _documentDetailState = MutableStateFlow(DocumentDetailState())
    val documentDetailState: StateFlow<DocumentDetailState> = _documentDetailState.asStateFlow()

    // State for folder operations
    private val _folderState = MutableStateFlow(FolderState())
    val folderState: StateFlow<FolderState> = _folderState.asStateFlow()

    // Current team ID
    private var currentTeamId: String = ""

    // Current folder ID (null for root)
    private var currentFolderId: String? = null

    // Current document ID
    private var currentDocumentId: String = ""

    /**
     * Initialize with team ID
     */
    fun initialize(teamId: String) {
        currentTeamId = teamId
        loadDocuments()
        loadFolders()
    }

    /**
     * Load documents for current team and folder
     */
    fun loadDocuments() {
        viewModelScope.launch {
            _documentListState.update { it.copy(isLoading = true, error = null) }

            if (currentFolderId == null) {
                documentRepository.getRootDocuments(currentTeamId).collectLatest { result ->
                    when (result) {
                        is Resource.Success -> {
                            _documentListState.update {
                                it.copy(
                                    documents = result.data,
                                    isLoading = false,
                                    error = null
                                )
                            }
                        }
                        is Resource.Error -> {
                            _documentListState.update {
                                it.copy(
                                    isLoading = false,
                                    error = result.message
                                )
                            }
                        }
                        is Resource.Loading -> {
                            _documentListState.update { it.copy(isLoading = true) }
                        }
                    }
                }
            } else {
                documentRepository.getDocumentsByFolder(currentFolderId!!).collectLatest { result ->
                    when (result) {
                        is Resource.Success -> {
                            _documentListState.update {
                                it.copy(
                                    documents = result.data,
                                    isLoading = false,
                                    error = null
                                )
                            }
                        }
                        is Resource.Error -> {
                            _documentListState.update {
                                it.copy(
                                    isLoading = false,
                                    error = result.message
                                )
                            }
                        }
                        is Resource.Loading -> {
                            _documentListState.update { it.copy(isLoading = true) }
                        }
                    }
                }
            }
        }
    }

    /**
     * Load folders for current team and parent folder
     */
    fun loadFolders() {
        viewModelScope.launch {
            _folderState.update { it.copy(isLoading = true, error = null) }

            if (currentFolderId == null) {
                documentRepository.getRootFolders(currentTeamId).collectLatest { result ->
                    when (result) {
                        is Resource.Success -> {
                            _folderState.update {
                                it.copy(
                                    folders = result.data,
                                    isLoading = false,
                                    error = null
                                )
                            }
                        }
                        is Resource.Error -> {
                            _folderState.update {
                                it.copy(
                                    isLoading = false,
                                    error = result.message
                                )
                            }
                        }
                        is Resource.Loading -> {
                            _folderState.update { it.copy(isLoading = true) }
                        }
                    }
                }
            } else {
                documentRepository.getSubfolders(currentFolderId!!).collectLatest { result ->
                    when (result) {
                        is Resource.Success -> {
                            _folderState.update {
                                it.copy(
                                    folders = result.data,
                                    isLoading = false,
                                    error = null
                                )
                            }
                        }
                        is Resource.Error -> {
                            _folderState.update {
                                it.copy(
                                    isLoading = false,
                                    error = result.message
                                )
                            }
                        }
                        is Resource.Loading -> {
                            _folderState.update { it.copy(isLoading = true) }
                        }
                    }
                }
            }

            // Also load parent folder if we're in a subfolder
            if (currentFolderId != null) {
                val result = documentRepository.getFolderById(currentFolderId!!)
                if (result is Resource.Success) {
                    _folderState.update {
                        it.copy(
                            currentFolder = result.data,
                            parentFolderId = result.data.parentFolderId
                        )
                    }
                }
            } else {
                _folderState.update {
                    it.copy(
                        currentFolder = null,
                        parentFolderId = null
                    )
                }
            }
        }
    }

    /**
     * Navigate to folder
     */
    fun navigateToFolder(folderId: String?) {
        currentFolderId = folderId
        loadDocuments()
        loadFolders()
    }

    /**
     * Navigate up to parent folder
     */
    fun navigateUp() {
        currentFolderId = _folderState.value.parentFolderId
        loadDocuments()
        loadFolders()
    }

    /**
     * Create a new folder
     */
    fun createFolder(name: String, description: String) {
        viewModelScope.launch {
            _folderState.update { it.copy(isCreating = true, error = null) }

            val folder = DocumentFolder(
                id = "",
                name = name,
                description = description,
                teamId = currentTeamId,
                parentFolderId = currentFolderId,
                createdBy = userRepository.getCurrentUserId(),
                createdAt = Date(),
                updatedAt = Date()
            )

            val result = documentRepository.createFolder(folder)
            when (result) {
                is Resource.Success -> {
                    _folderState.update {
                        it.copy(
                            isCreating = false,
                            error = null
                        )
                    }
                    loadFolders()
                }
                is Resource.Error -> {
                    _folderState.update {
                        it.copy(
                            isCreating = false,
                            error = result.message
                        )
                    }
                }
                is Resource.Loading -> {
                    _folderState.update { it.copy(isCreating = true) }
                }
            }
        }
    }

    /**
     * Delete a folder
     */
    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            _folderState.update { it.copy(isLoading = true, error = null) }

            val result = documentRepository.deleteFolder(folderId)
            when (result) {
                is Resource.Success -> {
                    _folderState.update {
                        it.copy(
                            isLoading = false,
                            error = null
                        )
                    }
                    loadFolders()
                }
                is Resource.Error -> {
                    _folderState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                is Resource.Loading -> {
                    _folderState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    /**
     * Load document detail
     */
    fun loadDocumentDetail(documentId: String) {
        currentDocumentId = documentId
        viewModelScope.launch {
            _documentDetailState.update { it.copy(isLoading = true, error = null) }

            documentRepository.getDocumentByIdFlow(documentId).collectLatest { result ->
                when (result) {
                    is Resource.Success -> {
                        _documentDetailState.update {
                            it.copy(
                                document = result.data,
                                isLoading = false,
                                error = null
                            )
                        }
                        loadDocumentVersions(documentId)
                        loadDocumentPermissions(documentId)
                    }
                    is Resource.Error -> {
                        _documentDetailState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                    is Resource.Loading -> {
                        _documentDetailState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    /**
     * Load document versions
     */
    private fun loadDocumentVersions(documentId: String) {
        viewModelScope.launch {
            documentRepository.getVersionsByDocument(documentId).collectLatest { result ->
                when (result) {
                    is Resource.Success -> {
                        _documentDetailState.update {
                            it.copy(
                                versions = result.data,
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _documentDetailState.update {
                            it.copy(
                                error = result.message
                            )
                        }
                    }
                    is Resource.Loading -> {
                        // Do nothing
                    }
                }
            }
        }
    }

    /**
     * Load document permissions
     */
    private fun loadDocumentPermissions(documentId: String) {
        viewModelScope.launch {
            documentRepository.getPermissionsByDocument(documentId).collectLatest { result ->
                when (result) {
                    is Resource.Success -> {
                        _documentDetailState.update {
                            it.copy(
                                permissions = result.data,
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _documentDetailState.update {
                            it.copy(
                                error = result.message
                            )
                        }
                    }
                    is Resource.Loading -> {
                        // Do nothing
                    }
                }
            }
        }
    }

    /**
     * Create a new document
     */
    fun createDocument(name: String, description: String, uri: Uri, accessLevel: String, allowedUsers: List<String> = emptyList()) {
        viewModelScope.launch {
            _documentListState.update { it.copy(isUploading = true, error = null) }

            try {
                // Copy the file from URI to a temporary file
                val inputStream = context.contentResolver.openInputStream(uri)
                val fileName = "temp_${java.util.UUID.randomUUID()}"
                val file = File(context.cacheDir, fileName)

                FileOutputStream(file).use { outputStream ->
                    inputStream?.copyTo(outputStream)
                }

                // Get file type and size
                val fileType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                val fileSize = file.length()

                // Create document
                val document = Document(
                    id = "",
                    name = name,
                    description = description,
                    teamId = currentTeamId,
                    folderId = currentFolderId,
                    fileUrl = "",
                    fileType = fileType,
                    fileSize = fileSize,
                    uploadedBy = userRepository.getCurrentUserId(),
                    uploadedAt = Date(),
                    lastModified = Date(),
                    accessLevel = accessLevel,
                    allowedUsers = allowedUsers
                )

                val result = documentRepository.createDocument(document, file)
                when (result) {
                    is Resource.Success -> {
                        _documentListState.update {
                            it.copy(
                                isUploading = false,
                                error = null
                            )
                        }
                        loadDocuments()
                    }
                    is Resource.Error -> {
                        _documentListState.update {
                            it.copy(
                                isUploading = false,
                                error = result.message
                            )
                        }
                    }
                    is Resource.Loading -> {
                        _documentListState.update { it.copy(isUploading = true) }
                    }
                }

                // Clean up
                file.delete()
            } catch (e: Exception) {
                _documentListState.update {
                    it.copy(
                        isUploading = false,
                        error = "Failed to upload file: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Update document
     */
    fun updateDocument(document: Document) {
        viewModelScope.launch {
            _documentDetailState.update { it.copy(isLoading = true, error = null) }

            val result = documentRepository.updateDocument(document)
            when (result) {
                is Resource.Success -> {
                    _documentDetailState.update {
                        it.copy(
                            isLoading = false,
                            error = null
                        )
                    }
                }
                is Resource.Error -> {
                    _documentDetailState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                is Resource.Loading -> {
                    _documentDetailState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    /**
     * Delete document
     */
    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            _documentDetailState.update { it.copy(isLoading = true, error = null) }

            val result = documentRepository.deleteDocument(documentId)
            when (result) {
                is Resource.Success -> {
                    _documentDetailState.update {
                        it.copy(
                            isLoading = false,
                            error = null
                        )
                    }
                    loadDocuments()
                }
                is Resource.Error -> {
                    _documentDetailState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                is Resource.Loading -> {
                    _documentDetailState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    /**
     * Download document
     */
    fun downloadDocument(documentId: String, versionId: String? = null): StateFlow<Resource<File>> {
        val downloadState = MutableStateFlow<Resource<File>>(Resource.Loading())

        viewModelScope.launch {
            val result = documentRepository.downloadDocument(documentId, versionId)
            downloadState.value = result
        }

        return downloadState.asStateFlow()
    }

    /**
     * Create a new version
     */
    fun createVersion(documentId: String, changeNotes: String, uri: Uri) {
        viewModelScope.launch {
            _documentDetailState.update { it.copy(isUploading = true, error = null) }

            try {
                // Copy the file from URI to a temporary file
                val inputStream = context.contentResolver.openInputStream(uri)
                val fileName = "temp_${UuidGenerator.generateUuid()}"
                val file = File(context.cacheDir, fileName)

                FileOutputStream(file).use { outputStream ->
                    inputStream?.copyTo(outputStream)
                }

                // Get file type and size
                val fileType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                val fileSize = file.length()

                // Create version
                val version = DocumentVersion(
                    id = "",
                    documentId = documentId,
                    versionNumber = 0, // Will be set by repository
                    fileUrl = "",
                    fileSize = fileSize,
                    uploadedBy = userRepository.getCurrentUserId(),
                    uploadedAt = Date(),
                    changeNotes = changeNotes
                )

                val result = documentRepository.createVersion(version, file)
                when (result) {
                    is Resource.Success -> {
                        _documentDetailState.update {
                            it.copy(
                                isUploading = false,
                                error = null
                            )
                        }
                        loadDocumentDetail(documentId)
                    }
                    is Resource.Error -> {
                        _documentDetailState.update {
                            it.copy(
                                isUploading = false,
                                error = result.message
                            )
                        }
                    }
                    is Resource.Loading -> {
                        _documentDetailState.update { it.copy(isUploading = true) }
                    }
                }

                // Clean up
                file.delete()
            } catch (e: Exception) {
                _documentDetailState.update {
                    it.copy(
                        isUploading = false,
                        error = "Failed to upload file: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Create a new permission
     */
    fun createPermission(documentId: String, userId: String, permissionType: String) {
        viewModelScope.launch {
            _documentDetailState.update { it.copy(isLoading = true, error = null) }

            val permission = DocumentPermission(
                id = "",
                documentId = documentId,
                userId = userId,
                permissionType = permissionType,
                grantedBy = userRepository.getCurrentUserId(),
                grantedAt = Date()
            )

            val result = documentRepository.createPermission(permission)
            when (result) {
                is Resource.Success -> {
                    _documentDetailState.update {
                        it.copy(
                            isLoading = false,
                            error = null
                        )
                    }
                    loadDocumentPermissions(documentId)
                }
                is Resource.Error -> {
                    _documentDetailState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                is Resource.Loading -> {
                    _documentDetailState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    /**
     * Delete permission
     */
    fun deletePermission(documentId: String, userId: String) {
        viewModelScope.launch {
            _documentDetailState.update { it.copy(isLoading = true, error = null) }

            val result = documentRepository.deletePermission(documentId, userId)
            when (result) {
                is Resource.Success -> {
                    _documentDetailState.update {
                        it.copy(
                            isLoading = false,
                            error = null
                        )
                    }
                    loadDocumentPermissions(documentId)
                }
                is Resource.Error -> {
                    _documentDetailState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                is Resource.Loading -> {
                    _documentDetailState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    /**
     * Search documents
     */
    fun searchDocuments(query: String) {
        viewModelScope.launch {
            _documentListState.update { it.copy(isLoading = true, error = null) }

            documentRepository.searchDocuments(currentTeamId, query).collectLatest { result ->
                when (result) {
                    is Resource.Success -> {
                        _documentListState.update {
                            it.copy(
                                documents = result.data,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _documentListState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                    is Resource.Loading -> {
                        _documentListState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    /**
     * Sync documents
     */
    fun syncDocuments() {
        viewModelScope.launch {
            _documentListState.update { it.copy(isSyncing = true, error = null) }

            val result = documentRepository.syncDocuments()
            when (result) {
                is Resource.Success -> {
                    _documentListState.update {
                        it.copy(
                            isSyncing = false,
                            error = null
                        )
                    }
                    loadDocuments()
                    loadFolders()
                    if (currentDocumentId.isNotEmpty()) {
                        loadDocumentDetail(currentDocumentId)
                    }
                }
                is Resource.Error -> {
                    _documentListState.update {
                        it.copy(
                            isSyncing = false,
                            error = result.message
                        )
                    }
                }
                is Resource.Loading -> {
                    _documentListState.update { it.copy(isSyncing = true) }
                }
            }
        }
    }
}
