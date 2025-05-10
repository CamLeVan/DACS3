package com.example.taskapplication.ui.team.document

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.taskapplication.domain.model.Document
import com.example.taskapplication.domain.model.DocumentFolder
import com.example.taskapplication.ui.animation.AnimationUtils
import com.example.taskapplication.ui.components.ErrorText
import com.example.taskapplication.ui.components.LoadingIndicator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen for displaying documents
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    navController: NavController,
    teamId: String,
    viewModel: DocumentViewModel = hiltViewModel()
) {
    val documentListState by viewModel.documentListState.collectAsState()
    val folderState by viewModel.folderState.collectAsState()

    val context = LocalContext.current

    // Dialog states
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showCreateDocumentDialog by remember { mutableStateOf(false) }
    var showDeleteFolderDialog by remember { mutableStateOf<DocumentFolder?>(null) }
    var showDeleteDocumentDialog by remember { mutableStateOf<Document?>(null) }

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    // File picker
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            showCreateDocumentDialog = true
            selectedFileUri = it
        }
    }

    // Initialize
    LaunchedEffect(teamId) {
        viewModel.initialize(teamId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Documents",
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (folderState.currentFolder != null) {
                            Text(
                                text = folderState.currentFolder?.name ?: "",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (folderState.currentFolder != null) {
                        IconButton(onClick = { viewModel.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { isSearching = !isSearching }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { viewModel.syncDocuments() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync")
                    }
                }
            )
        },
        floatingActionButton = {
            Column {
                // Add document FAB
                FloatingActionButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Document")
                }

                // Add folder FAB
                FloatingActionButton(
                    onClick = { showCreateFolderDialog = true }
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "Create Folder")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Search bar
            if (isSearching) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        if (it.isNotEmpty()) {
                            viewModel.searchDocuments(it)
                        } else {
                            viewModel.loadDocuments()
                        }
                    },
                    label = { Text("Search documents") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                )
            }

            // Loading indicator
            if (documentListState.isLoading || folderState.isLoading) {
                LoadingIndicator()
            }

            // Error message
            if (documentListState.error != null) {
                ErrorText(text = documentListState.error!!)
            }

            if (folderState.error != null) {
                ErrorText(text = folderState.error!!)
            }

            // Folders
            if (folderState.folders.isNotEmpty()) {
                Text(
                    text = "Folders",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxWidth()
                ) {
                    items(folderState.folders) { folder ->
                        FolderItem(
                            folder = folder,
                            onFolderClick = { viewModel.navigateToFolder(folder.id) },
                            onDeleteClick = { showDeleteFolderDialog = folder }
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Documents
            Text(
                text = "Documents",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (documentListState.documents.isEmpty() && !documentListState.isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No documents found",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(documentListState.documents) { document ->
                        DocumentItem(
                            document = document,
                            onDocumentClick = {
                                navController.navigate("document_detail/${teamId}/${document.id}")
                            },
                            onDeleteClick = { showDeleteDocumentDialog = document }
                        )
                    }
                }
            }

            // Syncing indicator
            if (documentListState.isSyncing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Syncing...",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    // Create folder dialog
    if (showCreateFolderDialog) {
        com.example.taskapplication.ui.team.document.CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onCreateFolder = { name, description ->
                viewModel.createFolder(name, description)
                showCreateFolderDialog = false
            }
        )
    }

    // Create document dialog
    if (showCreateDocumentDialog && selectedFileUri != null) {
        com.example.taskapplication.ui.team.document.CreateDocumentDialog(
            onDismiss = {
                showCreateDocumentDialog = false
                selectedFileUri = null
            },
            onCreateDocument = { name, description, accessLevel, allowedUsers ->
                selectedFileUri?.let { uri ->
                    // Chuyển đổi Uri thành File
                    val file = DocumentUtils.uriToFile(context, uri)
                    if (file != null) {
                        viewModel.createDocument(name, description, file, accessLevel, allowedUsers)
                    }
                }
                showCreateDocumentDialog = false
                selectedFileUri = null
            }
        )
    }

    // Delete folder dialog
    showDeleteFolderDialog?.let { folder ->
        AlertDialog(
            onDismissRequest = { showDeleteFolderDialog = null },
            title = { Text("Delete Folder") },
            text = { Text("Are you sure you want to delete the folder '${folder.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFolder(folder.id)
                        showDeleteFolderDialog = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteFolderDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete document dialog
    showDeleteDocumentDialog?.let { document ->
        AlertDialog(
            onDismissRequest = { showDeleteDocumentDialog = null },
            title = { Text("Delete Document") },
            text = { Text("Are you sure you want to delete the document '${document.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDocument(document.id)
                        showDeleteDocumentDialog = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDocumentDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
