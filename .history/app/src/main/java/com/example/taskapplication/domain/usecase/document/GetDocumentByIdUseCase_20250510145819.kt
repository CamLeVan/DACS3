package com.example.taskapplication.domain.usecase.document

import com.example.taskapplication.domain.model.Document
import com.example.taskapplication.domain.repository.DocumentRepository
import com.example.taskapplication.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDocumentByIdUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    operator fun invoke(documentId: String): Flow<Resource<Document>> {
        return repository.getDocumentByIdFlow(documentId)
    }
} 