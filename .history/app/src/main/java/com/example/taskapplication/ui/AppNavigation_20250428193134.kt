package com.example.taskapplication.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.taskapplication.ui.auth.AuthScreen
import com.example.taskapplication.ui.onboarding.OnboardingScreen
import com.example.taskapplication.ui.splash.SplashScreen
import com.example.taskapplication.ui.splash.SplashViewModel

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    var startDestination by remember { mutableStateOf("splash") }
    val splashViewModel: SplashViewModel = hiltViewModel()
    val isLoggedIn by splashViewModel.isLoggedIn.collectAsState()
    val isOnboardingCompleted by splashViewModel.isOnboardingCompleted.collectAsState()

    LaunchedEffect(isLoggedIn, isOnboardingCompleted) {
        if (isLoggedIn != null) {
            startDestination = when {
                !isOnboardingCompleted -> "onboarding"
                isLoggedIn == true -> "main"
                else -> "auth"
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("splash") {
            SplashScreen()
        }

        composable("onboarding") {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(if (isLoggedIn == true) "main" else "auth") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("auth") {
            AuthScreen(
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }

        composable("main") {
            MainScreen(
                onLogout = {
                    navController.navigate("auth") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }
    }
}
