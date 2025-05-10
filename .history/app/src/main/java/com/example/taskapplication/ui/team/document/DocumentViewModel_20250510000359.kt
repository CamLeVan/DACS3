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
}
