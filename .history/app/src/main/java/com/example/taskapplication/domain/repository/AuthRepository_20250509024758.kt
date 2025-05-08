package com.example.taskapplication.domain.repository

import com.example.taskapplication.domain.model.User

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(name: String, email: String, password: String): Result<User>
    suspend fun loginWithGoogle(token: String): Result<User>
    suspend fun logout(): Result<Unit>
    suspend fun isLoggedIn(): Boolean
    suspend fun getCurrentUser(): User?
    suspend fun registerBiometric(deviceId: String, biometricType: String, publicKey: String): Result<Unit>
    suspend fun verifyBiometric(deviceId: String, biometricType: String, signature: String): Result<User>
}
