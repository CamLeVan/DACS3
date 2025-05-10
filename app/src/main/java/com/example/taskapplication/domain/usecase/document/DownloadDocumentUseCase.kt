package com.example.taskapplication.domain.usecase.document

import com.example.taskapplication.domain.repository.DocumentRepository
import com.example.taskapplication.util.Resource
import java.io.File
import javax.inject.Inject

class DownloadDocumentUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(documentId: String, versionId: String? = null): Resource<File> {
        return repository.downloadDocument(documentId, versionId)
    }
} 