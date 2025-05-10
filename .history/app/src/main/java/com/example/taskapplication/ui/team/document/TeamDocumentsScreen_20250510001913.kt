package com.example.taskapplication.ui.team.document

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

/**
 * Wrapper for DocumentsScreen to maintain backward compatibility
 */
@Composable
fun TeamDocumentsScreen(
    teamId: String,
    onBackClick: () -> Unit
) {
    // Create a NavController for DocumentsScreen
    val navController = rememberNavController()

    // Use the new DocumentsScreen
    DocumentsScreen(
        navController = navController,
        teamId = teamId
    )
}
