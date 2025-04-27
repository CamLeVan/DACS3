package com.example.taskapplication.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit
) {
    var isLoginScreen by remember { mutableStateOf(true) }
    
    if (isLoginScreen) {
        LoginScreen(
            onLoginSuccess = onLoginSuccess,
            onNavigateToRegister = { isLoginScreen = false }
        )
    } else {
        RegisterScreen(
            onRegisterSuccess = onLoginSuccess,
            onNavigateToLogin = { isLoginScreen = true }
        )
    }
}
