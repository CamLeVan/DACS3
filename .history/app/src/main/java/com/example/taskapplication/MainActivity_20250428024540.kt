package com.example.taskapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.taskapplication.ui.AppNavigation
import com.example.taskapplication.ui.auth.AuthEvent
import com.example.taskapplication.ui.auth.AuthViewModel
import com.example.taskapplication.ui.theme.TaskApplicationTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.web_client_id)) // Add this line to request ID token
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Set up the ActivityResultLauncher for Google Sign In
        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleGoogleSignInResult(task)
        }

        // Observe auth events
        lifecycleScope.launch {
            authViewModel.authEvent.collectLatest { event ->
                when (event) {
                    is AuthEvent.GoogleSignInRequested -> {
                        launchGoogleSignIn()
                    }
                    else -> {}
                }
            }
        }

        setContent {
            TaskApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    private fun launchGoogleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            // Google Sign In was successful, authenticate with the server
            val idToken = account.idToken
            if (idToken != null) {
                Log.d(TAG, "Google Sign In successful, token: ${idToken.take(10)}...")
                lifecycleScope.launch {
                    authViewModel.loginWithGoogle(idToken)
                }
            } else {
                Log.e(TAG, "Google Sign In failed: ID Token is null")
                Toast.makeText(this, "Google Sign In failed: ID Token is null", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            // Google Sign In failed
            Log.e(TAG, "Google Sign In failed", e)
            Toast.makeText(this, "Google Sign In failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}