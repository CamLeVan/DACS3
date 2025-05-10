package com.example.taskapplication.ui.team.document

import com.example.taskapplication.domain.model.Document
import com.example.taskapplication.domain.model.DocumentFolder
import com.example.taskapplication.domain.model.DocumentPermission
import com.example.taskapplication.domain.model.DocumentVersion

/**
 * State for document list
 */
data class DocumentListState(
    val documents: List<Document> = emptyList(),
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val isSyncing: Boolean = false,
    val error: String? = null
)

/**
 * State for document detail
 */
data class DocumentDetailState(
    val document: Document? = null,
    val versions: List<DocumentVersion> = emptyList(),
    val permissions: List<DocumentPermission> = emptyList(),
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val error: String? = null
)

/**
 * State for folder operations
 */
data class FolderState(
    val folders: List<DocumentFolder> = emptyList(),
    val currentFolder: DocumentFolder? = null,
    val parentFolderId: String? = null,
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val error: String? = null
)
