package com.example.taskapplication.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.RoomDatabase
import com.example.taskapplication.ui.auth.AuthScreen
import com.example.taskapplication.ui.error.DatabaseErrorActivity
import com.example.taskapplication.ui.theme.TaskApplicationTheme
import com.example.taskapplication.workers.NotificationWorker
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationWorker.createNotificationChannel(this)
        }

        // Xử lý lỗi migration cơ sở dữ liệu
        try {
            setContent {
            TaskApplicationTheme {
                val navController = rememberNavController()
                val isLoggedIn by viewModel.isLoggedIn.collectAsState()

                NavHost(
                    navController = navController,
                    startDestination = if (isLoggedIn) "main" else "auth"
                ) {
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
                                viewModel.logout()
                                navController.navigate("auth") {
                                    popUpTo("main") { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
        } catch (e: Exception) {
            // Xử lý lỗi migration cơ sở dữ liệu
            if (e.message?.contains("Migration") == true ||
                e.message?.contains("team_documents") == true) {
                // Chuyển đến màn hình xử lý lỗi cơ sở dữ liệu
                Toast.makeText(this, "Lỗi cơ sở dữ liệu: ${e.message}", Toast.LENGTH_LONG).show()
                val intent = Intent(this, DatabaseErrorActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                // Xử lý các lỗi khác
                Toast.makeText(this, "Đã xảy ra lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }
}
