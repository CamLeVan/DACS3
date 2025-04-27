package com.example.taskapplication.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskapplication.domain.model.User
import com.example.taskapplication.domain.usecase.auth.GoogleSignInUseCase
import com.example.taskapplication.domain.usecase.auth.LoginUseCase
import com.example.taskapplication.domain.usecase.auth.RegisterUseCase
import com.example.taskapplication.util.ApiTester
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val googleSignInUseCase: GoogleSignInUseCase
) : ViewModel() {

    private val TAG = "AuthViewModel"

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _authEvent = MutableSharedFlow<AuthEvent>()
    val authEvent: SharedFlow<AuthEvent> = _authEvent.asSharedFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            // Test API directly first
            val apiTestResult = ApiTester.testLogin(email, password)
            Log.d(TAG, "API Test Result: $apiTestResult")

            val result = loginUseCase(email, password)

            result.fold(
                onSuccess = { user ->
                    _uiState.value = AuthUiState.Success(user)
                    _authEvent.emit(AuthEvent.LoginSuccess)
                },
                onFailure = { error ->
                    Log.e(TAG, "Login error: ${error.message}", error)
                    _uiState.value = AuthUiState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            val result = registerUseCase(name, email, password)

            result.fold(
                onSuccess = { user ->
                    _uiState.value = AuthUiState.Success(user)
                    _authEvent.emit(AuthEvent.RegisterSuccess)
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }

    fun loginWithGoogle(idToken: String? = null) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            if (idToken == null) {
                // This is just a placeholder for the UI
                // The actual Google Sign-In flow will be handled by the Activity
                _uiState.value = AuthUiState.Idle
                _authEvent.emit(AuthEvent.GoogleSignInRequested)
                return@launch
            }

            try {
                val result = googleSignInUseCase(idToken)

                result.fold(
                    onSuccess = { user ->
                        _uiState.value = AuthUiState.Success(user)
                        _authEvent.emit(AuthEvent.LoginSuccess)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Google sign in error", error)
                        _uiState.value = AuthUiState.Error(error.message ?: "Google sign in failed")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Google sign in exception", e)
                _uiState.value = AuthUiState.Error("Google sign in failed: ${e.message}")
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: User) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

sealed class AuthEvent {
    object LoginSuccess : AuthEvent()
    object RegisterSuccess : AuthEvent()
    object GoogleSignInRequested : AuthEvent()
}
