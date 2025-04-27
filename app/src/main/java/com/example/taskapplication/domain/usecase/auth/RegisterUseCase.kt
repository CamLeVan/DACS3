package com.example.taskapplication.domain.usecase.auth

import com.example.taskapplication.domain.model.User
import com.example.taskapplication.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(name: String, email: String, password: String): Result<User> {
        return authRepository.register(name, email, password)
    }
}
