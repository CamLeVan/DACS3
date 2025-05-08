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
            Log.d(TAG, "Attempting login with email: $email")
            val deviceId = getOrCreateDeviceId()
            val deviceName = getDeviceName()
            val loginRequest = LoginRequest(email, password, deviceId, deviceName)
            Log.d(TAG, "Login request: $loginRequest")

            val response = apiService.login(loginRequest)
            Log.d(TAG, "Login response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                Log.d(TAG, "Login successful, token: ${authResponse.token.take(10)}...")

                // Lưu token authentication
                dataStoreManager.saveAuthToken(authResponse.token)
                val user = authResponse.user.toDomainModel()
                dataStoreManager.saveUserInfo(user)

                Result.success(user)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Login failed: $errorBody")
                Result.failure(Exception(errorBody))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login error", e)
            Result.failure(e)
        }
    }

    override suspend fun register(name: String, email: String, password: String): Result<User> {
        return try {
            Log.d(TAG, "Attempting register with email: $email, name: $name")
            val deviceId = getOrCreateDeviceId()
            val deviceName = getDeviceName()
            val registerRequest = RegisterRequest(name, email, password, password, deviceId, deviceName)
            Log.d(TAG, "Register request: $registerRequest")

            val response = apiService.register(registerRequest)
            Log.d(TAG, "Register response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                Log.d(TAG, "Register successful, token: ${authResponse.token.take(10)}...")

                // Lưu token authentication
                dataStoreManager.saveAuthToken(authResponse.token)
                val user = authResponse.user.toDomainModel()
                dataStoreManager.saveUserInfo(user)

                Result.success(user)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Register failed: $errorBody")
                Result.failure(Exception(errorBody))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Register error", e)
            Result.failure(e)
        }
    }

    private suspend fun getOrCreateDeviceId(): String {
        var deviceId = dataStoreManager.deviceId.first()
        if (deviceId == null) {
            deviceId = java.util.UUID.randomUUID().toString()
            dataStoreManager.saveDeviceId(deviceId)
        }
        return deviceId
    }

    private fun getDeviceName(): String {
        return android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL
    }

    override suspend fun loginWithGoogle(token: String): Result<User> {
        return try {
            Log.d(TAG, "Attempting Google login with token: ${token.take(10)}...")
            val deviceId = getOrCreateDeviceId()
            val deviceName = getDeviceName()
            val googleAuthRequest = GoogleAuthRequest(token, deviceId, deviceName)
            Log.d(TAG, "Google login request: $googleAuthRequest")

            val response = apiService.loginWithGoogle(googleAuthRequest)
            Log.d(TAG, "Google login response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                Log.d(TAG, "Google login successful, token: ${authResponse.token.take(10)}...")

                // Lưu token authentication
                dataStoreManager.saveAuthToken(authResponse.token)
                val user = authResponse.user.toDomainModel()
                dataStoreManager.saveUserInfo(user)

                Result.success(user)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Google login failed: $errorBody")
                Result.failure(Exception(errorBody))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google login error", e)
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            val deviceId = getOrCreateDeviceId()
            val response = apiService.logout(deviceId)

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
