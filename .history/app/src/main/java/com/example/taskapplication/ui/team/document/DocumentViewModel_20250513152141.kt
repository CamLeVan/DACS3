package com.example.taskapplication.ui.team.document

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskapplication.domain.model.Document
import com.example.taskapplication.domain.model.DocumentFolder
import com.example.taskapplication.domain.model.DocumentPermission
import com.example.taskapplication.domain.model.DocumentVersion
import com.example.taskapplication.domain.usecase.document.*
import com.example.taskapplication.domain.usecase.document.permission.*
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

import javax.inject.Inject

/**
 * ViewModel for document operations
 */
@HiltViewModel
class DocumentViewModel @Inject constructor(
    private val getDocumentsUseCase: GetDocumentsUseCase,
    private val getDocumentByIdUseCase: GetDocumentByIdUseCase,
    private val createDocumentUseCase: CreateDocumentUseCase,
    private val updateDocumentUseCase: UpdateDocumentUseCase,
    private val deleteDocumentUseCase: DeleteDocumentUseCase,
    private val downloadDocumentUseCase: DownloadDocumentUseCase,
    private val getDocumentVersionsUseCase: GetDocumentVersionsUseCase,
    private val createDocumentVersionUseCase: CreateDocumentVersionUseCase,
    private val getFoldersUseCase: GetFoldersUseCase,
    private val createFolderUseCase: CreateFolderUseCase,
    private val updateFolderUseCase: UpdateFolderUseCase,
    private val deleteFolderUseCase: DeleteFolderUseCase,
    private val syncDocumentsUseCase: SyncDocumentsUseCase,
    private val getDocumentPermissionsUseCase: GetDocumentPermissionsUseCase,
    private val createDocumentPermissionUseCase: CreateDocumentPermissionUseCase,
    private val deleteDocumentPermissionUseCase: DeleteDocumentPermissionUseCase,
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

            getDocumentsUseCase(currentTeamId, currentFolderId).collectLatest { result ->
                when (result) {
                    is Resource.Success -> {
                        _documentListState.update {
                            it.copy(
                                documents = result.data ?: emptyList(),
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
                    else -> {
                        // Không xử lý các trường hợp khác
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

            getFoldersUseCase(currentTeamId, currentFolderId).collectLatest { result ->
                when (result) {
                    is Resource.Success -> {
                        _folderState.update {
                            it.copy(
                                folders = result.data ?: emptyList(),
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
                    else -> {
                        // Không xử lý các trường hợp khác
                    }
                }
            }
        }
    }

    /**
     * Load document detail
     */
    fun loadDocumentDetail(documentId: String) {
        viewModelScope.launch {
            _documentDetailState.update { it.copy(isLoading = true, error = null) }

            getDocumentByIdUseCase(documentId).collectLatest { result ->
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
                    else -> {
                        // Không xử lý các trường hợp khác
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
            getDocumentVersionsUseCase(documentId).collectLatest { result ->
                when (result) {
                    is Resource.Success -> {
                        _documentDetailState.update {
                            it.copy(versions = result.data ?: emptyList())
                        }
                    }
                    is Resource.Error -> {
                        // Không cập nhật error state vì đây là thông tin phụ
                    }
                    is Resource.Loading -> {
                        // Không cập nhật loading state vì đây là thông tin phụ
                    }
                    else -> {
                        // Không xử lý các trường hợp khác
                    }
                }
            }
        }
    }

    /**
     * Create a new document
     */
    fun createDocument(name: String, description: String, file: File, accessLevel: String = "team", allowedUsers: List<String> = emptyList()) {
        viewModelScope.launch {
            _documentListState.update { it.copy(isLoading = true, error = null) }

            val document = Document(
                id = UuidGenerator.generate(),
                name = name,
                description = description,
                teamId = currentTeamId,
                folderId = currentFolderId,
                fileUrl = "",
                fileType = file.extension,
                fileSize = file.length(),
                uploadedBy = "",
                uploadedAt = System.currentTimeMillis(),
                lastModified = System.currentTimeMillis(),
                accessLevel = accessLevel,
                allowedUsers = allowedUsers
            )

            when (val result = createDocumentUseCase(document, file)) {
                is Resource.Success -> {
                    loadDocuments()
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
                else -> {
                    // Không xử lý các trường hợp khác
                }
            }
        }
    }

    /**
     * Update a document
     */
    fun updateDocument(document: Document) {
        viewModelScope.launch {
            _documentDetailState.update { it.copy(isLoading = true, error = null) }

            when (val result = updateDocumentUseCase(document)) {
                is Resource.Success -> {
                    loadDocumentDetail(document.id)
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
                else -> {
                    // Không xử lý các trường hợp khác
                }
            }
        }
    }

    /**
     * Update a document with specific fields
     */
    fun updateDocument(documentId: String, name: String, description: String) {
        viewModelScope.launch {
            _documentDetailState.update { it.copy(isLoading = true, error = null) }

            // Get current document
            val currentDocument = _documentDetailState.value.document

            if (currentDocument != null) {
                // Create updated document
                val updatedDocument = currentDocument.copy(
                    name = name,
                    description = description,
                    lastModified = System.currentTimeMillis()
                )

                // Update document
                updateDocument(updatedDocument)
            } else {
                // If document is not in state, try to load it first
                getDocumentByIdUseCase(documentId).collectLatest { result ->
                    when (result) {
                        is Resource.Success -> {
                            val document = result.data
                            if (document != null) {
                                val updatedDocument = document.copy(
                                    name = name,
                                    description = description,
                                    lastModified = System.currentTimeMillis()
                                )
                                updateDocument(updatedDocument)
                            } else {
                                _documentDetailState.update {
                                    it.copy(
                                        isLoading = false,
                                        error = "Không tìm thấy tài liệu"
                                    )
                                }
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
                            // Already set loading state
                        }
                        else -> {
                            // Không xử lý các trường hợp khác
                        }
                    }
                }
            }
        }
    }

    /**
     * Delete a document
     */
    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            _documentListState.update { it.copy(isLoading = true, error = null) }

            when (val result = deleteDocumentUseCase(documentId)) {
                is Resource.Success -> {
                    loadDocuments()
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
                else -> {
                    // Không xử lý các trường hợp khác
                }
            }
        }
    }

    /**
     * Download a document
     */
    suspend fun downloadDocument(documentId: String, versionId: String? = null): Resource<File> {
        return downloadDocumentUseCase(documentId, versionId)
    }

    /**
     * Create a new version
     */
    fun createVersion(documentId: String, file: File, changeNotes: String) {
        viewModelScope.launch {
            _documentDetailState.update { it.copy(isLoading = true, error = null) }

            val version = DocumentVersion(
                id = UuidGenerator.generate(),
                documentId = documentId,
                versionNumber = _documentDetailState.value.versions.size + 1,
                fileUrl = "",
                fileSize = file.length(),
                uploadedBy = "",
                uploadedAt = System.currentTimeMillis(),
                changeNotes = changeNotes
            )

            when (val result = createDocumentVersionUseCase(version, file)) {
                is Resource.Success -> {
                    loadDocumentDetail(documentId)
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
                else -> {
                    // Không xử lý các trường hợp khác
                }
            }
        }
    }

    /**
     * Create a new folder
     */
    fun createFolder(name: String, description: String) {
        viewModelScope.launch {
            _folderState.update { it.copy(isLoading = true, error = null) }

            val folder = DocumentFolder(
                id = UuidGenerator.generate(),
                name = name,
                description = description,
                teamId = currentTeamId,
                parentFolderId = currentFolderId,
                createdBy = "",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            when (val result = createFolderUseCase(folder)) {
                is Resource.Success -> {
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
                else -> {
                    // Không xử lý các trường hợp khác
                }
            }
        }
    }

    /**
     * Update a folder
     */
    fun updateFolder(folder: DocumentFolder) {
        viewModelScope.launch {
            _folderState.update { it.copy(isLoading = true, error = null) }

            when (val result = updateFolderUseCase(folder)) {
                is Resource.Success -> {
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
                else -> {
                    // Không xử lý các trường hợp khác
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

            when (val result = deleteFolderUseCase(folderId)) {
                is Resource.Success -> {
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
                else -> {
                    // Không xử lý các trường hợp khác
                }
            }
        }
    }

    /**
     * Navigate to a folder
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
        currentFolderId = _folderState.value.currentFolder?.parentFolderId
        loadDocuments()
        loadFolders()
    }

    /**
     * Sync documents
     */
    fun syncDocuments() {
        viewModelScope.launch {
            _documentListState.update { it.copy(isLoading = true, error = null) }

            when (val result = syncDocumentsUseCase()) {
                is Resource.Success -> {
                    loadDocuments()
                    loadFolders()
                }
                is Resource.Error -> {
                    // Xử lý thông báo lỗi cụ thể
                    val errorMessage = when {
                        result.message.contains("network", ignoreCase = true) ->
                            "Không có kết nối mạng. Vui lòng kiểm tra kết nối và thử lại."
                        result.message.contains("timeout", ignoreCase = true) ->
                            "Kết nối đến máy chủ quá chậm. Vui lòng thử lại sau."
                        result.message.contains("server", ignoreCase = true) ->
                            "Máy chủ đang gặp sự cố. Vui lòng thử lại sau."
                        else -> result.message
                    }

                    _documentListState.update {
                        it.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                    }
                }
                is Resource.Loading -> {
                    _documentListState.update { it.copy(isLoading = true) }
                }
                else -> {
                    // Không xử lý các trường hợp khác
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

            if (query.isBlank()) {
                // Nếu query trống, tải lại tất cả tài liệu
                loadDocuments()
                return@launch
            }

            // Sử dụng repository để tìm kiếm
            getDocumentsUseCase.searchDocuments(currentTeamId, query).collectLatest { result ->
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
                        // Xử lý thông báo lỗi cụ thể cho tìm kiếm
                        val errorMessage = when {
                            result.message.contains("network", ignoreCase = true) ->
                                "Không thể tìm kiếm: Không có kết nối mạng."
                            query.length < 2 ->
                                "Vui lòng nhập ít nhất 2 ký tự để tìm kiếm."
                            else -> "Không thể tìm kiếm: ${result.message}"
                        }

                        _documentListState.update {
                            it.copy(
                                isLoading = false,
                                error = errorMessage
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
     * Load document permissions
     */
    private fun loadDocumentPermissions(documentId: String) {
        viewModelScope.launch {
            getDocumentPermissionsUseCase(documentId).collectLatest { result ->
                when (result) {
                    is Resource.Success -> {
                        _documentDetailState.update {
                            it.copy(permissions = result.data)
                        }
                    }
                    is Resource.Error -> {
                        // Không cập nhật error state vì đây là thông tin phụ
                    }
                    is Resource.Loading -> {
                        // Không cập nhật loading state vì đây là thông tin phụ
                    }
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
                id = UuidGenerator.generate(),
                documentId = documentId,
                userId = userId,
                permissionType = permissionType,
                grantedBy = "", // Sẽ được cập nhật bởi repository
                grantedAt = System.currentTimeMillis()
            )

            when (val result = createDocumentPermissionUseCase(permission)) {
                is Resource.Success -> {
                    loadDocumentPermissions(documentId)
                    _documentDetailState.update { it.copy(isLoading = false) }
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
     * Delete a permission
     */
    fun deletePermission(documentId: String, userId: String) {
        viewModelScope.launch {
            _documentDetailState.update { it.copy(isLoading = true, error = null) }

            when (val result = deleteDocumentPermissionUseCase(documentId, userId)) {
                is Resource.Success -> {
                    loadDocumentPermissions(documentId)
                    _documentDetailState.update { it.copy(isLoading = false) }
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
     * Download document with callback
     */
    fun downloadDocument(documentId: String, versionId: String? = null, callback: (Result<File>) -> Unit) {
        viewModelScope.launch {
            try {
                val result = downloadDocumentUseCase(documentId, versionId)
                when (result) {
                    is Resource.Success -> {
                        val file = result.data
                        if (file != null) {
                            callback(Result.success(file))
                        } else {
                            callback(Result.failure(Exception("Không tìm thấy tập tin")))
                        }
                    }
                    is Resource.Error -> {
                        callback(Result.failure(Exception(result.message)))
                    }
                    is Resource.Loading -> {
                        // Ignore loading state
                    }
                }
            } catch (e: Exception) {
                callback(Result.failure(e))
            }
        }
    }
}
