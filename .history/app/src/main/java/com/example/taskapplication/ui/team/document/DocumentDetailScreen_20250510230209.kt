package com.example.taskapplication.ui.team.document

import android.content.Intent
import android.webkit.MimeTypeMap
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

    val documentDetailState = viewModel.documentDetailState.collectAsState().value
    val document = documentDetailState.document
    val versions = documentDetailState.versions
    val isLoading = documentDetailState.isLoading
    val error = documentDetailState.error

    LaunchedEffect(documentId) {
        viewModel.loadDocumentDetail(documentId)
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
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                LoadingIndicator()
            } else if (error != null) {
                ErrorText(error)
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    TabRow(selectedTabIndex = selectedTabIndex) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = { Text("Thông tin") }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = { Text("Phiên bản") }
                        )
                        Tab(
                            selected = selectedTabIndex == 2,
                            onClick = { selectedTabIndex = 2 },
                            text = { Text("Quyền truy cập") }
                        )
                    }

                    when (selectedTabIndex) {
                        0 -> DocumentInfoTab(document)
                        1 -> DocumentVersionsTab(
                            versions = versions,
                            onDownload = { versionId ->
                                scope.launch {
                                    val result = viewModel.downloadDocument(documentId, versionId)
                                    if (result is Resource.Success && result.data != null) {
                                        val file = result.data
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.provider",
                                            file
                                        )
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, getMimeType(file))
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            }
                        )
                        2 -> DocumentPermissionsTab(
                            permissions = documentDetailState.permissions,
                            onDeletePermission = { permission ->
                                scope.launch {
                                    viewModel.deletePermission(documentId, permission.userId)
                                }
                            }
                        )
                    }
                }
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

    if (showEditDialog && document != null) {
        EditDocumentDialog(
            document = document,
            onDismiss = { showEditDialog = false },
            onSave = { name, description ->
                viewModel.updateDocument(documentId, name, description)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun DocumentInfoTab(document: Document?) {
    if (document == null) {
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            InfoItem("Name", document.name)
            InfoItem("Description", document.description ?: "")
            InfoItem("File Type", document.fileType)
            InfoItem("File Size", document.getFormattedFileSize())
            InfoItem("Uploaded At", DateConverter.formatLong(document.uploadedAt))
            InfoItem("Last Modified", DateConverter.formatLong(document.lastModified))
        }
    }
}

@Composable
private fun DocumentVersionsTab(
    versions: List<DocumentVersion>,
    onDownload: (String) -> Unit
) {
    if (versions.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No versions available")
        }
        return
    }

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
                    text = "Version ${version.versionNumber}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = DateConverter.formatLong(version.uploadedAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Change Notes: ${version.changeNotes}",
                    style = MaterialTheme.typography.bodySmall,
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
private fun EditDocumentDialog(
    document: Document,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(document.name) }
    var description by remember { mutableStateOf(document.description ?: "") }

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