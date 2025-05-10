package com.example.taskapplication.domain.usecase.document

import com.example.taskapplication.domain.model.DocumentVersion
import com.example.taskapplication.domain.repository.DocumentRepository
import com.example.taskapplication.util.Resource
import java.io.File
import javax.inject.Inject

class CreateDocumentVersionUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(version: DocumentVersion, file: File): Resource<DocumentVersion> {
        return repository.createVersion(version, file)
    }
} 