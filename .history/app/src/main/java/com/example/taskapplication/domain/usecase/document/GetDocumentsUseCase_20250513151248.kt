package com.example.taskapplication.domain.usecase.document

import com.example.taskapplication.domain.model.Document
import com.example.taskapplication.domain.repository.DocumentRepository
import com.example.taskapplication.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDocumentsUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    operator fun invoke(teamId: String, folderId: String? = null): Flow<Resource<List<Document>>> {
        return if (folderId != null) {
            repository.getDocumentsByFolder(folderId)
        } else {
            repository.getDocumentsByTeam(teamId)
        }
    }

    /**
     * Tìm kiếm tài liệu theo từ khóa
     */
    fun searchDocuments(teamId: String, query: String): Flow<Resource<List<Document>>> {
        return repository.searchDocuments(teamId, query)
    }
}