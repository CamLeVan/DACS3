package com.example.taskapplication.domain.usecase

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.domain.repository.AuthRepository
import kotlinx.coroutines.flow.first
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import javax.inject.Inject

class BiometricRegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val dataStoreManager: DataStoreManager
) {
    private val TAG = "BiometricRegisterUseCase"
    private val KEY_ALIAS = "biometric_auth_key"
    private val ANDROID_KEYSTORE = "AndroidKeyStore"

    suspend operator fun invoke(biometricType: String): Result<Unit> {
        try {
            // Kiểm tra xem người dùng đã đăng nhập chưa
            if (!authRepository.isLoggedIn()) {
                return Result.failure(Exception("Người dùng chưa đăng nhập"))
            }

            // Tạo cặp khóa mới
            val keyPair = generateKeyPair()
            val publicKey = keyPair.public
            val publicKeyString = Base64.encodeToString(publicKey.encoded, Base64.DEFAULT)

            // Lấy device_id
            val deviceId = dataStoreManager.deviceId.first() ?: return Result.failure(Exception("Device ID không tồn tại"))

            // Gọi API để đăng ký sinh trắc học
            val result = Result.success(Unit) // Biometric đã bị loại bỏ khỏi API

            // Lưu thông tin sinh trắc học vào DataStore nếu thành công
            if (result.isSuccess) {
                dataStoreManager.saveBiometricEnabled(true)
                dataStoreManager.saveBiometricType(biometricType)
            }

            return result
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi đăng ký sinh trắc học", e)
            return Result.failure(e)
        }
    }

    private fun generateKeyPair(): KeyPair {
        // Xóa khóa cũ nếu tồn tại
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS)
        }

        // Tạo cặp khóa mới
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE
        )

        val parameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        ).apply {
            setDigests(KeyProperties.DIGEST_SHA256)
            setUserAuthenticationRequired(true)
            setInvalidatedByBiometricEnrollment(true)
        }.build()

        keyPairGenerator.initialize(parameterSpec)
        return keyPairGenerator.generateKeyPair()
    }

    fun getPublicKey(): PublicKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
            entry?.certificate?.publicKey
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi lấy khóa công khai", e)
            null
        }
    }

    fun getPrivateKey(): PrivateKey? {
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
