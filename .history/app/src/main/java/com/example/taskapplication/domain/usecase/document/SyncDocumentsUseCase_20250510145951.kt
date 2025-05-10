package com.example.taskapplication.domain.usecase.document

import com.example.taskapplication.domain.repository.DocumentRepository
import com.example.taskapplication.util.Resource
import javax.inject.Inject

class SyncDocumentsUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(): Resource<Unit> {
        return repository.syncDocuments()
    }
} 