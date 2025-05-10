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
import kotlinx.coroutines.launch
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
import com.example.taskapplication.util.DateConverter

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
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            showAddVersionDialog = true
        }
    }

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

/**
 * Download document
 */
private fun downloadDocument(
    viewModel: DocumentViewModel,
    documentId: String,
    versionId: String?,
    context: android.content.Context
) {
    // Just call the function directly
    val result = viewModel.downloadDocument(documentId, versionId).value
    when (result) {
        is com.example.taskapplication.util.Resource.Success -> {
            val file = result.data
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
        }
        is com.example.taskapplication.util.Resource.Error -> {
            // Show error
        }
        is com.example.taskapplication.util.Resource.Loading -> {
            // Show loading
        }
    }
}

/**
 * Document details tab
 */
@Composable
private fun DocumentDetailsTab(document: Document?) {
    if (document == null) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Document info
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Document Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                InfoRow("Name", document.name)
                InfoRow("Description", document.description)
                InfoRow("File Type", document.fileType)
                InfoRow("File Size", document.getFormattedFileSize())
                InfoRow("Created", DateConverter.formatLong(document.uploadedAt))
                InfoRow("Modified", DateConverter.formatLong(document.lastModified))
                InfoRow("Access Level", document.accessLevel)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Uploaded by info
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Uploaded By",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User",
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = document.uploadedBy,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

/**
 * Document versions tab
 */
@Composable
private fun VersionsTab(
    versions: List<DocumentVersion>,
    onDownloadClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (versions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No versions available",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            versions.forEach { version ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Version ${version.versionNumber}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            IconButton(onClick = { onDownloadClick(version.id) }) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = "Download Version"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = version.changeNotes,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row {
                            Text(
                                text = version.getFormattedFileSize(),
                                style = MaterialTheme.typography.bodySmall
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "Uploaded: ${DateConverter.formatLong(version.uploadedAt)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Document permissions tab
 */
@Composable
private fun PermissionsTab(
    permissions: List<DocumentPermission>,
    onDeleteClick: (DocumentPermission) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (permissions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No permissions set",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            permissions.forEach { permission ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "User",
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Column {
                                Text(
                                    text = permission.userId,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Text(
                                    text = "Permission: ${permission.permissionType}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        IconButton(onClick = { onDeleteClick(permission) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Permission"
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Info row for document details
 */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(120.dp)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Edit document dialog
 */
@Composable
fun EditDocumentDialog(
    document: Document,
    onDismiss: () -> Unit,
    onUpdateDocument: (Document) -> Unit
) {
    var documentName by remember { mutableStateOf(document.name) }
    var documentDescription by remember { mutableStateOf(document.description) }
    var accessLevel by remember { mutableStateOf(document.accessLevel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Document") },
        text = {
            Column {
                OutlinedTextField(
                    value = documentName,
                    onValueChange = { documentName = it },
                    label = { Text("Document Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = documentDescription,
                    onValueChange = { documentDescription = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Access Level",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AccessLevelOption(
                        label = "Team",
                        selected = accessLevel == "team",
                        onClick = { accessLevel = "team" }
                    )

                    AccessLevelOption(
                        label = "Private",
                        selected = accessLevel == "private",
                        onClick = { accessLevel = "private" }
                    )

                    AccessLevelOption(
                        label = "Public",
                        selected = accessLevel == "public",
                        onClick = { accessLevel = "public" }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedDocument = document.copy(
                        name = documentName,
                        description = documentDescription,
                        accessLevel = accessLevel
                    )
                    onUpdateDocument(updatedDocument)
                },
                enabled = documentName.isNotBlank()
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Add version dialog
 */
@Composable
fun AddVersionDialog(
    onDismiss: () -> Unit,
    onAddVersion: (changeNotes: String) -> Unit
) {
    var changeNotes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Version") },
        text = {
            Column {
                OutlinedTextField(
                    value = changeNotes,
                    onValueChange = { changeNotes = it },
                    label = { Text("Change Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAddVersion(changeNotes) },
                enabled = changeNotes.isNotBlank()
            ) {
                Text("Upload")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Add permission dialog
 */
@Composable
fun AddPermissionDialog(
    onDismiss: () -> Unit,
    onAddPermission: (userId: String, permissionType: String) -> Unit
) {
    var userId by remember { mutableStateOf("") }
    var permissionType by remember { mutableStateOf("view") } // Default to view

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Permission") },
        text = {
            Column {
                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it },
                    label = { Text("User ID") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Permission Type",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AccessLevelOption(
                        label = "View",
                        selected = permissionType == "view",
                        onClick = { permissionType = "view" }
                    )

                    AccessLevelOption(
                        label = "Edit",
                        selected = permissionType == "edit",
                        onClick = { permissionType = "edit" }
                    )

                    AccessLevelOption(
                        label = "Admin",
                        selected = permissionType == "admin",
                        onClick = { permissionType = "admin" }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAddPermission(userId, permissionType) },
                enabled = userId.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Extension function to capitalize first letter
 */
private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

/**
 * Format date for display
 */
private fun formatDate(timestamp: Long): String {
    return DateConverter.formatLong(timestamp)
}