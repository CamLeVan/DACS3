package com.example.taskapplication.ui.team.document

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.taskapplication.util.DateConverter
import com.example.taskapplication.util.Resource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    navController: NavController,
    documentId: String,
    viewModel: DocumentViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showAddPermissionDialog by remember { mutableStateOf(false) }
    
    val document by viewModel.document.collectAsState()
    val versions by viewModel.versions.collectAsState()
    val permissions by viewModel.permissions.collectAsState()

    LaunchedEffect(documentId) {
        viewModel.loadDocument(documentId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(document?.name ?: "Document Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showAddPermissionDialog = true }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Permission")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Information") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Versions") }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Access") }
                )
            }

            when (selectedTabIndex) {
                0 -> DocumentInfoTab(document)
                1 -> DocumentVersionsTab(versions, onDownload = { versionId ->
                    scope.launch {
                        when (val result = viewModel.downloadDocument(documentId, versionId)) {
                            is Resource.Success -> {
                                result.data?.let { file ->
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        file
                                    )
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "application/pdf")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(intent)
                                }
                            }
                            is Resource.Error -> {
                                // Show error message
                            }
                            is Resource.Loading -> {
                                // Show loading indicator
                            }
                        }
                    }
                })
                2 -> DocumentAccessTab(
                    permissions = permissions,
                    onCreatePermission = { userId, permission ->
                        scope.launch {
                            viewModel.updateDocumentAccess(documentId, userId, permission)
                        }
                    },
                    onDeletePermission = { userId ->
                        scope.launch {
                            viewModel.deleteDocumentAccess(documentId, userId)
                        }
                    }
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Document") },
            text = { Text("Are you sure you want to delete this document?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.deleteDocument(documentId)
                            navController.navigateUp()
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditDialog) {
        EditDocumentDialog(
            document = document,
            onDismiss = { showEditDialog = false },
            onSave = { name, description ->
                scope.launch {
                    viewModel.updateDocument(documentId, name, description)
                    showEditDialog = false
                }
            }
        )
    }

    if (showAddPermissionDialog) {
        AddPermissionDialog(
            onDismiss = { showAddPermissionDialog = false },
            onAdd = { userId, permission ->
                scope.launch {
                    viewModel.updateDocumentAccess(documentId, userId, permission)
                    showAddPermissionDialog = false
                }
            }
        )
    }
}

@Composable
private fun DocumentInfoTab(document: Document?) {
    if (document == null) {
        LoadingIndicator()
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            InfoItem("Name", document.name)
            InfoItem("Description", document.description ?: "No description")
            InfoItem("Created", DateConverter.formatDate(document.createdAt))
            InfoItem("Updated", DateConverter.formatDate(document.updatedAt))
            InfoItem("Size", formatFileSize(document.size))
            InfoItem("Type", document.fileType)
        }
    }
}

@Composable
private fun DocumentVersionsTab(
    versions: List<DocumentVersion>,
    onDownload: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(versions) { version ->
            VersionItem(version, onDownload)
        }
    }
}

@Composable
private fun DocumentAccessTab(
    permissions: List<DocumentPermission>,
    onCreatePermission: (String, DocumentPermission) -> Unit,
    onDeletePermission: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(permissions) { permission ->
            PermissionItem(
                permission = permission,
                onDelete = { onDeletePermission(permission.userId) }
            )
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun VersionItem(
    version: DocumentVersion,
    onDownload: (String) -> Unit
) {
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
            Column {
                Text(
                    text = "Version ${version.version}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = DateConverter.formatDate(version.createdAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { onDownload(version.id) }) {
                Icon(Icons.Default.Download, contentDescription = "Download")
            }
        }
    }
}

@Composable
private fun PermissionItem(
    permission: DocumentPermission,
    onDelete: () -> Unit
) {
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
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column {
                    Text(
                        text = permission.userName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = permission.permission.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Remove Permission")
            }
        }
    }
}

@Composable
private fun EditDocumentDialog(
    document: Document?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(document?.name ?: "") }
    var description by remember { mutableStateOf(document?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Document") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, description) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AddPermissionDialog(
    onDismiss: () -> Unit,
    onAdd: (String, DocumentPermission) -> Unit
) {
    var userId by remember { mutableStateOf("") }
    var selectedPermission by remember { mutableStateOf(DocumentPermission.READ) }

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
                Spacer(modifier = Modifier.height(8.dp))
                // Add permission selection dropdown
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(userId, selectedPermission) }
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