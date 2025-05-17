package com.example.taskapplication.domain.usecase.auth

import com.example.taskapplication.domain.model.User
import com.example.taskapplication.domain.repository.AuthRepository
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): User? {
        val result = authRepository.getCurrentUser()
        return if (result.isSuccess) result.getOrNull() else null
    }
}
