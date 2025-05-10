package com.example.taskapplication.ui.team.document

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.taskapplication.domain.model.Document
import com.example.taskapplication.domain.model.DocumentPermission
import com.example.taskapplication.domain.model.DocumentVersion
import com.example.taskapplication.ui.components.ErrorText
import com.example.taskapplication.ui.components.LoadingIndicator
import kotlinx.coroutines.flow.collectLatest
import java.io.File

/**
 * Screen for displaying document details
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    navController: NavController,
    teamId: String,
    documentId: String,
    viewModel: DocumentViewModel = hiltViewModel()
) {
    val documentDetailState by viewModel.documentDetailState.collectAsState()
    val context = LocalContext.current
    
    // Tab state
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Details", "Versions", "Permissions")
    
    // Dialog states
    var showEditDocumentDialog by remember { mutableStateOf(false) }
    var showAddVersionDialog by remember { mutableStateOf(false) }
    var showAddPermissionDialog by remember { mutableStateOf(false) }
    var showDeletePermissionDialog by remember { mutableStateOf<DocumentPermission?>(null) }
    
    // File picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            showAddVersionDialog = true
        }
    }
    
    // Selected file URI
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    
    // Initialize
    LaunchedEffect(documentId) {
        viewModel.loadDocumentDetail(documentId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = documentDetailState.document?.name ?: "Document Details",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        documentDetailState.document?.let { document ->
                            downloadDocument(viewModel, document.id, null, context)
                        }
                    }) {
                        Icon(Icons.Default.Download, contentDescription = "Download")
                    }
                    
                    IconButton(onClick = { showEditDocumentDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    
                    IconButton(onClick = {
                        documentDetailState.document?.let { document ->
                            viewModel.deleteDocument(document.id)
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTabIndex == 1) {
                // Add version FAB
                FloatingActionButton(onClick = { filePickerLauncher.launch("*/*") }) {
                    Icon(Icons.Default.FileUpload, contentDescription = "Upload New Version")
                }
            } else if (selectedTabIndex == 2) {
                // Add permission FAB
                FloatingActionButton(onClick = { showAddPermissionDialog = true }) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Permission")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Loading indicator
            if (documentDetailState.isLoading) {
                LoadingIndicator()
            }
            
            // Error message
            if (documentDetailState.error != null) {
                ErrorText(text = documentDetailState.error!!)
            }
            
            // Tabs
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }
            
            // Tab content
            when (selectedTabIndex) {
                0 -> DocumentDetailsTab(documentDetailState.document)
                1 -> VersionsTab(
                    versions = documentDetailState.versions,
                    onDownloadClick = { versionId ->
                        downloadDocument(viewModel, documentId, versionId, context)
                    }
                )
                2 -> PermissionsTab(
                    permissions = documentDetailState.permissions,
                    onDeleteClick = { permission ->
                        showDeletePermissionDialog = permission
                    }
                )
            }
        }
    }
    
    // Edit document dialog
    if (showEditDocumentDialog && documentDetailState.document != null) {
        EditDocumentDialog(
            document = documentDetailState.document!!,
            onDismiss = { showEditDocumentDialog = false },
            onUpdateDocument = { updatedDocument ->
                viewModel.updateDocument(updatedDocument)
                showEditDocumentDialog = false
            }
        )
    }
    
    // Add version dialog
    if (showAddVersionDialog && selectedFileUri != null) {
        AddVersionDialog(
            onDismiss = {
                showAddVersionDialog = false
                selectedFileUri = null
            },
            onAddVersion = { changeNotes ->
                selectedFileUri?.let {
                    viewModel.createVersion(documentId, changeNotes, it)
                }
                showAddVersionDialog = false
                selectedFileUri = null
            }
        )
    }
    
    // Add permission dialog
    if (showAddPermissionDialog) {
        AddPermissionDialog(
            onDismiss = { showAddPermissionDialog = false },
            onAddPermission = { userId, permissionType ->
                viewModel.createPermission(documentId, userId, permissionType)
                showAddPermissionDialog = false
            }
        )
    }
    
    // Delete permission dialog
    showDeletePermissionDialog?.let { permission ->
        AlertDialog(
            onDismissRequest = { showDeletePermissionDialog = null },
            title = { Text("Delete Permission") },
            text = { Text("Are you sure you want to remove permission for this user?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePermission(permission.documentId, permission.userId)
                        showDeletePermissionDialog = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePermissionDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
