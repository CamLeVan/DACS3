package com.example.taskapplication.ui.team.document

import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.taskapplication.ui.animation.AnimationUtils
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
    var showCreateVersionDialog by remember { mutableStateOf(false) }
    var showCreatePermissionDialog by remember { mutableStateOf(false) }
    var showDeletePermissionDialog by remember { mutableStateOf<DocumentPermission?>(null) }

    val documentDetailState = viewModel.documentDetailState.collectAsState().value
    val document = documentDetailState.document
    val versions = documentDetailState.versions
    val permissions = documentDetailState.permissions
    val isLoading = documentDetailState.isLoading
    val error = documentDetailState.error

    LaunchedEffect(documentId) {
        viewModel.loadDocumentDetail(documentId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(document?.name ?: "Chi tiết tài liệu") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Xóa")
                    }
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Chỉnh sửa")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTabIndex == 1) {
                FloatingActionButton(onClick = { showCreateVersionDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Thêm phiên bản")
                }
            } else if (selectedTabIndex == 2) {
                FloatingActionButton(onClick = { showCreatePermissionDialog = true }) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Thêm quyền truy cập")
                }
            }
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

                    AnimatedContent(
                        targetState = selectedTabIndex,
                        transitionSpec = { AnimationUtils.tabContentTransition(initialState, targetState) },
                        label = "Tab Content Animation"
                    ) { targetTabIndex ->
                        when (targetTabIndex) {
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
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xóa tài liệu") },
            text = { Text("Bạn có chắc chắn muốn xóa tài liệu này?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.deleteDocument(documentId)
                            navController.navigateUp()
                        }
                    }
                ) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Hủy")
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

    // Create version dialog
    if (showCreateVersionDialog) {
        CreateVersionDialog(
            documentId = documentId,
            onDismiss = { showCreateVersionDialog = false },
            onCreateVersion = { file, changeNotes ->
                scope.launch {
                    viewModel.createVersion(documentId, file, changeNotes)
                }
                showCreateVersionDialog = false
            }
        )
    }

    // Create permission dialog
    if (showCreatePermissionDialog) {
        CreatePermissionDialog(
            documentId = documentId,
            onDismiss = { showCreatePermissionDialog = false },
            onCreatePermission = { userId, permissionType ->
                viewModel.createPermission(documentId, userId, permissionType)
                showCreatePermissionDialog = false
            }
        )
    }

    // Delete permission dialog
    showDeletePermissionDialog?.let { permission ->
        AlertDialog(
            onDismissRequest = { showDeletePermissionDialog = null },
            title = { Text("Xóa quyền truy cập") },
            text = { Text("Bạn có chắc chắn muốn xóa quyền truy cập của người dùng này?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePermission(permission.documentId, permission.userId)
                        showDeletePermissionDialog = null
                    }
                ) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePermissionDialog = null }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
private fun DocumentInfoTab(document: Document?) {
    if (document == null) {
        return
    }

    val infoItems = listOf(
        "Tên" to document.name,
        "Mô tả" to (document.description ?: ""),
        "Loại file" to document.fileType,
        "Kích thước" to document.getFormattedFileSize(),
        "Ngày tải lên" to DateConverter.formatLong(document.uploadedAt),
        "Chỉnh sửa lần cuối" to DateConverter.formatLong(document.lastModified)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        itemsIndexed(infoItems) { index, (label, value) ->
            AnimatedVisibility(
                visible = true,
                enter = AnimationUtils.listItemEnterAnimation(index),
                exit = AnimationUtils.listItemExitAnimation
            ) {
                InfoItem(label, value)
            }
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
            Text("Không có phiên bản nào")
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        itemsIndexed(versions) { index, version ->
            AnimatedVisibility(
                visible = true,
                enter = AnimationUtils.listItemEnterAnimation(index),
                exit = AnimationUtils.listItemExitAnimation
            ) {
                VersionItem(version, onDownload)
            }
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
    var isHovered by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "Card Scale Animation"
    )
    val elevation by animateDpAsState(
        targetValue = if (isHovered) 4.dp else 1.dp,
        animationSpec = tween(durationMillis = 150),
        label = "Card Elevation Animation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.shadowElevation = elevation.toPx()
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        isHovered = event.type == PointerEventType.Enter
                    }
                }
            }
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
                    text = "Phiên bản ${version.versionNumber}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = DateConverter.formatLong(version.uploadedAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Ghi chú: ${version.changeNotes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { onDownload(version.id) },
                modifier = Modifier.scale(if (isHovered) 1.1f else 1f)
            ) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = "Tải xuống",
                    tint = if (isHovered) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
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
        title = { Text("Chỉnh sửa tài liệu") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mô tả") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, description) }
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

/**
 * Get MIME type from file
 */
private fun getMimeType(file: File): String {
    val extension = file.extension
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
}

/**
 * Document permissions tab
 */
@Composable
private fun DocumentPermissionsTab(
    permissions: List<DocumentPermission>,
    onDeletePermission: (DocumentPermission) -> Unit
) {
    if (permissions.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Không có quyền truy cập nào")
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        itemsIndexed(permissions) { index, permission ->
            AnimatedVisibility(
                visible = true,
                enter = AnimationUtils.listItemEnterAnimation(index),
                exit = AnimationUtils.listItemExitAnimation
            ) {
                PermissionItem(permission, onDeletePermission)
            }
        }
    }
}

/**
 * Permission item
 */
@Composable
private fun PermissionItem(
    permission: DocumentPermission,
    onDelete: (DocumentPermission) -> Unit
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
                    text = "Người dùng: ${permission.userId}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Quyền: ${getPermissionTypeText(permission.permissionType)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Cấp bởi: ${permission.grantedBy}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Ngày cấp: ${DateConverter.formatLong(permission.grantedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { onDelete(permission) }) {
                Icon(Icons.Default.Delete, contentDescription = "Xóa quyền")
            }
        }
    }
}

/**
 * Get permission type text
 */
private fun getPermissionTypeText(permissionType: String): String {
    return when (permissionType) {
        "view" -> "Xem"
        "edit" -> "Chỉnh sửa"
        "admin" -> "Quản trị"
        else -> permissionType
    }
}

