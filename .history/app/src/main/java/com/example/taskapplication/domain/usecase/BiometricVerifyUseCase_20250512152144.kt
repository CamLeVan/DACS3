package com.example.taskapplication.domain.usecase

import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.domain.model.User
import com.example.taskapplication.domain.repository.AuthRepository
import kotlinx.coroutines.flow.first
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import javax.inject.Inject

class BiometricVerifyUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val dataStoreManager: DataStoreManager
) {
    private val TAG = "BiometricVerifyUseCase"
    private val KEY_ALIAS = "biometric_auth_key"
    private val ANDROID_KEYSTORE = "AndroidKeyStore"

    suspend operator fun invoke(signature: String): Result<User> {
        try {
            // Lấy device_id
            val deviceId = dataStoreManager.deviceId.first() ?: return Result.failure(Exception("Device ID không tồn tại"))

            // Lấy biometric_type
            val biometricType = dataStoreManager.biometricType.first() ?: return Result.failure(Exception("Biometric type không tồn tại"))

            // Gọi API để xác thực sinh trắc học
            return authRepository.verifyBiometric(deviceId, biometricType, signature)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi xác minh sinh trắc học", e)
            return Result.failure(e)
        }
    }

    fun signData(data: String): String? {
        try {
            val privateKey = getPrivateKey() ?: return null

            val signature = Signature.getInstance("SHA256withRSA")
            signature.initSign(privateKey)
            signature.update(data.toByteArray())

            val signatureBytes = signature.sign()
            return Base64.encodeToString(signatureBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi ký dữ liệu", e)
            return null
        }
    }

    private fun getPrivateKey(): PrivateKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            keyStore.getKey(KEY_ALIAS, null) as? PrivateKey
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi lấy khóa riêng tư", e)
            null
        }
    }
}
