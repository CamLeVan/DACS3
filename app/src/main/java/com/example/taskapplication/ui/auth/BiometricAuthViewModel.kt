package com.example.taskapplication.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskapplication.domain.model.User
import com.example.taskapplication.domain.usecase.BiometricRegisterUseCase
import com.example.taskapplication.domain.usecase.BiometricVerifyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BiometricAuthViewModel @Inject constructor(
    private val biometricRegisterUseCase: BiometricRegisterUseCase,
    private val biometricVerifyUseCase: BiometricVerifyUseCase
) : ViewModel() {

    private val TAG = "BiometricAuthViewModel"

    private val _uiState = MutableStateFlow<BiometricAuthUiState>(BiometricAuthUiState.Idle)
    val uiState: StateFlow<BiometricAuthUiState> = _uiState.asStateFlow()

    fun registerBiometric(biometricType: String = "fingerprint") {
        viewModelScope.launch {
            _uiState.value = BiometricAuthUiState.Loading

            val result = biometricRegisterUseCase(biometricType)

            result.fold(
                onSuccess = {
                    _uiState.value = BiometricAuthUiState.RegisterSuccess
                    Log.d(TAG, "Biometric registration successful")
                },
                onFailure = { error ->
                    _uiState.value = BiometricAuthUiState.Error(error.message ?: "Unknown error")
                    Log.e(TAG, "Biometric registration failed", error)
                }
            )
        }
    }

    fun verifyBiometric() {
        viewModelScope.launch {
            _uiState.value = BiometricAuthUiState.Loading

            // Tạo chữ ký cho dữ liệu
            val dataToSign = "verify_biometric_${System.currentTimeMillis()}"
            val signature = biometricVerifyUseCase.signData(dataToSign)

            if (signature == null) {
                _uiState.value = BiometricAuthUiState.Error("Không thể tạo chữ ký sinh trắc học")
                return@launch
            }

            val result = biometricVerifyUseCase(signature)

            result.fold(
                onSuccess = { user ->
                    _uiState.value = BiometricAuthUiState.Success(user)
                    Log.d(TAG, "Biometric verification successful")
                },
                onFailure = { error ->
                    _uiState.value = BiometricAuthUiState.Error(error.message ?: "Unknown error")
                    Log.e(TAG, "Biometric verification failed", error)
                }
            )
        }
    }

    fun resetState() {
        _uiState.value = BiometricAuthUiState.Idle
    }
}

sealed class BiometricAuthUiState {
    object Idle : BiometricAuthUiState()
    object Loading : BiometricAuthUiState()
    object RegisterSuccess : BiometricAuthUiState()
    data class Success(val user: User) : BiometricAuthUiState()
    data class Error(val message: String) : BiometricAuthUiState()
}
