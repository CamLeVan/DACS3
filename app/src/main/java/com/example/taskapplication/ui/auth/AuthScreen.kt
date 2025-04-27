package com.example.taskapplication.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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

    AnimatedContent(
        targetState = isLoginScreen,
        transitionSpec = {
            (slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) + fadeIn(tween(300)))
                .togetherWith(
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left) + fadeOut(tween(300))
                )
        },
        label = "Auth Screen Animation"
    ) { isLogin ->
        if (isLogin) {
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
}
