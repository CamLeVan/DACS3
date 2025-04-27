package com.example.taskapplication.domain.usecase.auth

import com.example.taskapplication.domain.model.User
import com.example.taskapplication.domain.repository.AuthRepository
import javax.inject.Inject

class GoogleSignInUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(idToken: String): Result<User> {
        return authRepository.loginWithGoogle(idToken)
    }
}
