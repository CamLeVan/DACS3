package com.example.taskapplication.domain.usecase.document

import com.example.taskapplication.domain.model.DocumentFolder
import com.example.taskapplication.domain.repository.DocumentRepository
import com.example.taskapplication.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFoldersUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    operator fun invoke(teamId: String, parentId: String? = null): Flow<Resource<List<DocumentFolder>>> {
        return if (parentId != null) {
            repository.getSubfolders(parentId)
        } else {
            repository.getRootFolders(teamId)
        }
    }
} 