package com.example.taskapplication.domain.usecase.document.permission

import com.example.taskapplication.domain.repository.DocumentRepository
import com.example.taskapplication.util.Resource
import javax.inject.Inject

/**
 * Use case for deleting document permission
 */
class DeleteDocumentPermissionUseCase @Inject constructor(
    private val documentRepository: DocumentRepository
) {
    /**
     * Delete a permission
     */
    suspend operator fun invoke(documentId: String, userId: String): Resource<Unit> {
        return documentRepository.deletePermission(documentId, userId)
    }
}
