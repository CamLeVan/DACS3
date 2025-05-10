package com.example.taskapplication.domain.usecase.document.permission

import com.example.taskapplication.domain.model.DocumentPermission
import com.example.taskapplication.domain.repository.DocumentRepository
import com.example.taskapplication.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting document permissions
 */
class GetDocumentPermissionsUseCase @Inject constructor(
    private val documentRepository: DocumentRepository
) {
    /**
     * Get permissions for a document
     */
    operator fun invoke(documentId: String): Flow<Resource<List<DocumentPermission>>> {
        return documentRepository.getPermissionsByDocument(documentId)
    }
}
