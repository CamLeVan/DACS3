package com.example.taskapplication.ui.team.document

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.taskapplication.domain.model.TeamDocument
import com.example.taskapplication.ui.components.ConfirmationDialog
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDocumentsScreen(
    viewModel: TeamDocumentsViewModel = hiltViewModel(),
    teamId: String,
    onBackClick: () -> Unit
) {
    val documentsState by viewModel.documentsState.collectAsState()
    val uploadState by viewModel.uploadState.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val teamName by viewModel.teamName.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val selectedFileType by viewModel.selectedFileType.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // State for upload dialog
    var showUploadDialog by remember { mutableStateOf(false) }
    var documentToDelete by remember { mutableStateOf<TeamDocument?>(null) }
    var documentToChangeAccess by remember { mutableStateOf<TeamDocument?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }
    
    // File picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Handle file selection
            // In a real app, you would copy the file to your app's storage
            // For now, we'll just show a snackbar
            coroutineScope.launch {
                snackbarHostState.showSnackbar("File selected: ${uri.lastPathSegment}")
            }
        }
    }
    
    // Show error message in snackbar
    LaunchedEffect(uploadState) {
        if (uploadState is UploadState.Error) {
            snackbarHostState.showSnackbar(
                message = (uploadState as UploadState.Error).message
            )
            viewModel.resetUploadState()
        } else if (uploadState is UploadState.Success) {
            snackbarHostState.showSnackbar(
                message = "Document uploaded successfully"
            )
            viewModel.resetUploadState()
        }
    }
    
    // Handle download state
    LaunchedEffect(downloadState) {
        if (downloadState is DownloadState.Error) {
            snackbarHostState.showSnackbar(
                message = (downloadState as DownloadState.Error).message
            )
            viewModel.resetDownloadState()
        } else if (downloadState is DownloadState.Success) {
            val file = (downloadState as DownloadState.Success).file
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, context.contentResolver.getType(uri))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            viewModel.resetDownloadState()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(teamName ?: "Team Documents") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                    
                    Box {
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All") },
                                onClick = {
                                    viewModel.setFileTypeFilter(null)
                                    showFilterMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Documents") },
                                onClick = {
                                    viewModel.setFileTypeFilter("doc")
                                    showFilterMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Images") },
                                onClick = {
                                    viewModel.setFileTypeFilter("image")
                                    showFilterMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("PDFs") },
                                onClick = {
                                    viewModel.setFileTypeFilter("pdf")
                                    showFilterMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Videos") },
                                onClick = {
                                    viewModel.setFileTypeFilter("video")
                                    showFilterMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showUploadDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Upload") },
                text = { Text("Upload") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search documents") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            )
            
            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFileType == null,
                    onClick = { viewModel.setFileTypeFilter(null) },
                    label = { Text("All") }
                )
                
                FilterChip(
                    selected = selectedFileType == "doc",
                    onClick = { viewModel.setFileTypeFilter("doc") },
                    label = { Text("Docs") }
                )
                
                FilterChip(
                    selected = selectedFileType == "image",
                    onClick = { viewModel.setFileTypeFilter("image") },
                    label = { Text("Images") }
                )
                
                FilterChip(
                    selected = selectedFileType == "pdf",
                    onClick = { viewModel.setFileTypeFilter("pdf") },
                    label = { Text("PDFs") }
                )
                
                FilterChip(
                    selected = selectedFileType == "video",
                    onClick = { viewModel.setFileTypeFilter("video") },
                    label = { Text("Videos") }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Documents list
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                when (val state = documentsState) {
                    is DocumentsState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    
                    is DocumentsState.Empty -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "No documents yet",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Upload a document to get started",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    is DocumentsState.Success -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.documents) { document ->
                                DocumentItem(
                                    document = document,
                                    onDownloadClick = { viewModel.downloadDocument(document.id) },
                                    onDeleteClick = { documentToDelete = document },
                                    onAccessLevelClick = { documentToChangeAccess = document },
                                    isAdmin = isAdmin,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            
                            item {
                                Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
                            }
                        }
                    }
                    
                    is DocumentsState.Error -> {
                        Text(
                            text = "Error: ${state.message}",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }
    
    // Upload dialog
    if (showUploadDialog) {
        UploadDocumentDialog(
            onDismiss = { showUploadDialog = false },
            onUpload = { name, description, file, accessLevel, allowedUsers ->
                viewModel.uploadDocument(name, description, file, accessLevel, allowedUsers)
                showUploadDialog = false
            },
            onPickFile = {
                filePickerLauncher.launch("*/*")
            }
        )
    }
    
    // Delete confirmation dialog
    documentToDelete?.let { document ->
        ConfirmationDialog(
            title = "Delete Document",
            message = "Are you sure you want to delete '${document.name}'? This action cannot be undone.",
            confirmButtonText = "Delete",
            onConfirm = {
                viewModel.deleteDocument(document.id)
                documentToDelete = null
            },
            onDismiss = {
                documentToDelete = null
            }
        )
    }
    
    // Access level dialog
    documentToChangeAccess?.let { document ->
        DocumentAccessLevelDialog(
            document = document,
            onDismiss = { documentToChangeAccess = null },
            onSave = { accessLevel, allowedUsers ->
                viewModel.updateDocumentAccessLevel(document.id, accessLevel, allowedUsers)
                documentToChangeAccess = null
            }
        )
    }
}

@Composable
fun DocumentItem(
    document: TeamDocument,
    onDownloadClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onAccessLevelClick: () -> Unit,
    isAdmin: Boolean,
    modifier: Modifier = Modifier
) {
    val dateFormatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val formattedDate = dateFormatter.format(Date(document.uploadedAt))
    
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.clickable { onDownloadClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Document icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getDocumentIcon(document.fileType),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Document info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = document.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = document.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = document.getFormattedFileSize(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (document.accessLevel != "team") {
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Restricted access",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Actions
            IconButton(onClick = onDownloadClick) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download"
                )
            }
            
            if (isAdmin || document.uploadedBy == "current_user_id") { // Replace with actual current user ID check
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options"
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Change access") },
                            onClick = {
                                onAccessLevelClick()
                                showMenu = false
                            }
                        )
                        
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                onDeleteClick()
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun getDocumentIcon(fileType: String): ImageVector {
    return when (fileType) {
        "image" -> Icons.Default.Image
        "video" -> Icons.Default.VideoLibrary
        "pdf" -> Icons.Default.PictureAsPdf
        else -> Icons.Default.Description
    }
}
