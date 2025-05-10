package com.example.taskapplication.domain.usecase.document

import com.example.taskapplication.domain.model.DocumentVersion
import com.example.taskapplication.domain.repository.DocumentRepository
import com.example.taskapplication.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDocumentVersionsUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    operator fun invoke(documentId: String): Flow<Resource<List<DocumentVersion>>> {
        return repository.getVersionsByDocument(documentId)
    }
} 