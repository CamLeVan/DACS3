package com.example.taskapplication.domain.usecase.document

import com.example.taskapplication.domain.model.Document
import com.example.taskapplication.domain.repository.DocumentRepository
import com.example.taskapplication.util.Resource
import javax.inject.Inject

class UpdateDocumentUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(document: Document): Resource<Document> {
        return repository.updateDocument(document)
    }
} 