package com.example.taskapplication.data.repository

import android.util.Log
import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.api.request.GoogleAuthRequest
import com.example.taskapplication.data.api.request.LoginRequest
import com.example.taskapplication.data.api.request.RegisterRequest
import com.example.taskapplication.data.mapper.toDomainModel
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.domain.model.User
import com.example.taskapplication.domain.repository.AuthRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager
) : AuthRepository {

    private val TAG = "AuthRepository"

    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val response = apiService.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!

                // Lưu token authentication
                dataStoreManager.saveAuthToken(authResponse.token)
                val user = authResponse.user.toDomainModel()
                dataStoreManager.saveUserInfo(user)

                Result.success(user)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login error", e)
            Result.failure(e)
        }
    }

    override suspend fun register(name: String, email: String, password: String): Result<User> {
        return try {
            val response = apiService.register(RegisterRequest(name, email, password, password))
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!

                // Lưu token authentication
                dataStoreManager.saveAuthToken(authResponse.token)
                val user = authResponse.user.toDomainModel()
                dataStoreManager.saveUserInfo(user)

                Result.success(user)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Register error", e)
            Result.failure(e)
        }
    }

    override suspend fun loginWithGoogle(token: String): Result<User> {
        return try {
            val response = apiService.loginWithGoogle(GoogleAuthRequest(token))
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!

                // Lưu token authentication
                dataStoreManager.saveAuthToken(authResponse.token)
                val user = authResponse.user.toDomainModel()
                dataStoreManager.saveUserInfo(user)

                Result.success(user)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google login error", e)
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            val response = apiService.logout()

            // Clear local data
            dataStoreManager.clearAuthToken()
            dataStoreManager.clearUserInfo()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Logout error", e)
            // Logout thành công ngay cả khi API fails
            dataStoreManager.clearAuthToken()
            dataStoreManager.clearUserInfo()
            Result.success(Unit)
        }
    }

    override suspend fun isLoggedIn(): Boolean {
        val token = dataStoreManager.authToken.first()
        return token != null && token.isNotEmpty()
    }

    override suspend fun getCurrentUser(): User? {
        return dataStoreManager.userInfo.first()
    }
}
