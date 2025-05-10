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
import com.example.taskapplication.util.DateConverter
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
            getDocumentVersionsUseCase(documentId).collectLatest { result ->
                when (result) {
                    is Resource.Success -> {
                        _documentDetailState.update {
                            it.copy(versions = result.data)
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
     * Create a new document
     */
    fun createDocument(name: String, description: String, file: File) {
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
                accessLevel = "team"
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

    /**
     * Search documents
     */
    fun searchDocuments(query: String) {
        // TODO: Implement search functionality
    }
}
