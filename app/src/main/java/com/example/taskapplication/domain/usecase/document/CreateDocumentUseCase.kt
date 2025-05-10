package com.example.taskapplication.domain.usecase.document

import com.example.taskapplication.domain.model.Document
import com.example.taskapplication.domain.repository.DocumentRepository
import com.example.taskapplication.util.Resource
import java.io.File
import javax.inject.Inject

class CreateDocumentUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(document: Document, file: File): Resource<Document> {
        return repository.createDocument(document, file)
    }
} 