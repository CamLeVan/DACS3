package com.example.taskapplication.domain.usecase.document.permission

import com.example.taskapplication.domain.model.DocumentPermission
import com.example.taskapplication.domain.repository.DocumentRepository
import com.example.taskapplication.util.Resource
import javax.inject.Inject

/**
 * Use case for creating document permission
 */
class CreateDocumentPermissionUseCase @Inject constructor(
    private val documentRepository: DocumentRepository
) {
    /**
     * Create a new permission
     */
    suspend operator fun invoke(permission: DocumentPermission): Resource<DocumentPermission> {
        return documentRepository.createPermission(permission)
    }
}
