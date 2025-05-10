package com.example.taskapplication.domain.usecase.document

import com.example.taskapplication.domain.model.DocumentFolder
import com.example.taskapplication.domain.repository.DocumentRepository
import com.example.taskapplication.util.Resource
import javax.inject.Inject

class UpdateFolderUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(folder: DocumentFolder): Resource<DocumentFolder> {
        return repository.updateFolder(folder)
    }
} 