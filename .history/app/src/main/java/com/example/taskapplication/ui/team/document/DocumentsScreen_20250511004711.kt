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
import kotlinx.coroutines.delay
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
                        // Title with animation
                        val titleScale = remember { Animatable(0.8f) }

                        LaunchedEffect(Unit) {
                            titleScale.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }

                        Text(
                            text = "Tài liệu",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.graphicsLayer {
                                scaleX = titleScale.value
                                scaleY = titleScale.value
                            }
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
                    // Search button with animation
                    var isSearchHovered by remember { mutableStateOf(false) }
                    val searchScale by animateFloatAsState(
                        targetValue = if (isSearchHovered) 1.2f else 1f,
                        animationSpec = tween(150),
                        label = "Search Button Scale"
                    )

                    IconButton(
                        onClick = { isSearching = !isSearching },
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = searchScale
                                scaleY = searchScale
                            }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        isSearchHovered = event.type == PointerEventType.Enter || event.type == PointerEventType.Move
                                    }
                                }
                            }
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Tìm kiếm",
                            tint = if (isSearchHovered) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }

                    // Sync button with animation
                    var isSyncHovered by remember { mutableStateOf(false) }
                    val syncScale by animateFloatAsState(
                        targetValue = if (isSyncHovered) 1.2f else 1f,
                        animationSpec = tween(150),
                        label = "Sync Button Scale"
                    )
                    val syncRotation by animateFloatAsState(
                        targetValue = if (isSyncHovered) 180f else 0f,
                        animationSpec = tween(300),
                        label = "Sync Button Rotation"
                    )

                    IconButton(
                        onClick = { viewModel.syncDocuments() },
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = syncScale
                                scaleY = syncScale
                                rotationZ = syncRotation
                            }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        isSyncHovered = event.type == PointerEventType.Enter || event.type == PointerEventType.Move
                                    }
                                }
                            }
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Đồng bộ",
                            tint = if (isSyncHovered) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Column {
                // Add document FAB with animation
                val docFabScale = remember { Animatable(0.6f) }
                LaunchedEffect(Unit) {
                    delay(300) // Delay for staggered effect
                    docFabScale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }

                var isDocFabHovered by remember { mutableStateOf(false) }
                val docHoverScale by animateFloatAsState(
                    targetValue = if (isDocFabHovered) 1.1f else 1f,
                    animationSpec = tween(150),
                    label = "Doc FAB Hover Scale"
                )

                FloatingActionButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .graphicsLayer {
                            scaleX = docFabScale.value * docHoverScale
                            scaleY = docFabScale.value * docHoverScale
                        }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    isDocFabHovered = event.type == PointerEventType.Enter || event.type == PointerEventType.Move
                                }
                            }
                        }
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Thêm tài liệu",
                        modifier = Modifier.graphicsLayer {
                            rotationZ = if (isDocFabHovered) 90f else 0f
                        }
                    )
                }

                // Add folder FAB with animation
                val folderFabScale = remember { Animatable(0.6f) }
                LaunchedEffect(Unit) {
                    delay(500) // Delay for staggered effect
                    folderFabScale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }

                var isFolderFabHovered by remember { mutableStateOf(false) }
                val folderHoverScale by animateFloatAsState(
                    targetValue = if (isFolderFabHovered) 1.1f else 1f,
                    animationSpec = tween(150),
                    label = "Folder FAB Hover Scale"
                )

                FloatingActionButton(
                    onClick = { showCreateFolderDialog = true },
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = folderFabScale.value * folderHoverScale
                            scaleY = folderFabScale.value * folderHoverScale
                        }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    isFolderFabHovered = event.type == PointerEventType.Enter || event.type == PointerEventType.Move
                                }
                            }
                        }
                ) {
                    Icon(
                        Icons.Default.CreateNewFolder,
                        contentDescription = "Tạo thư mục",
                        modifier = Modifier.graphicsLayer {
                            rotationZ = if (isFolderFabHovered) 10f else 0f
                        }
                    )
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
                    label = { Text("Tìm kiếm tài liệu") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                )
            }

            // Loading indicator
            AnimatedVisibility(
                visible = documentListState.isLoading || folderState.isLoading,
                enter = fadeIn() + expandIn(),
                exit = fadeOut() + shrinkOut()
            ) {
                LoadingIndicator()
            }

            // Error message
            AnimatedVisibility(
                visible = documentListState.error != null,
                enter = fadeIn() + expandIn(),
                exit = fadeOut() + shrinkOut()
            ) {
                documentListState.error?.let { ErrorText(text = it) }
            }

            AnimatedVisibility(
                visible = folderState.error != null,
                enter = fadeIn() + expandIn(),
                exit = fadeOut() + shrinkOut()
            ) {
                folderState.error?.let { ErrorText(text = it) }
            }

            // Folders
            if (folderState.folders.isNotEmpty()) {
                Text(
                    text = "Thư mục",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxWidth()
                ) {
                    itemsIndexed(folderState.folders) { index, folder ->
                        AnimatedVisibility(
                            visible = true,
                            enter = AnimationUtils.listItemEnterAnimation(index),
                            exit = AnimationUtils.listItemExitAnimation
                        ) {
                            FolderItem(
                                folder = folder,
                                onFolderClick = { viewModel.navigateToFolder(folder.id) },
                                onDeleteClick = { showDeleteFolderDialog = folder }
                            )
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Documents
            Text(
                text = "Tài liệu",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.primary
            )

            if (documentListState.documents.isEmpty() && !documentListState.isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Không tìm thấy tài liệu nào",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    itemsIndexed(documentListState.documents) { index, document ->
                        AnimatedVisibility(
                            visible = true,
                            enter = AnimationUtils.listItemEnterAnimation(index),
                            exit = AnimationUtils.listItemExitAnimation
                        ) {
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
            }

            // Syncing indicator
            AnimatedVisibility(
                visible = documentListState.isSyncing,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val rotation by rememberInfiniteTransition().animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "Sync Indicator Rotation"
                    )

                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Đang đồng bộ",
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer { rotationZ = rotation },
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Đang đồng bộ...",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    // Create folder dialog
    AnimatedVisibility(
        visible = showCreateFolderDialog,
        enter = AnimationUtils.dialogEnterAnimation,
        exit = AnimationUtils.dialogExitAnimation
    ) {
        com.example.taskapplication.ui.team.document.CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onCreateFolder = { name, description ->
                viewModel.createFolder(name, description)
                showCreateFolderDialog = false
            }
        )
    }

    // Create document dialog
    AnimatedVisibility(
        visible = showCreateDocumentDialog && selectedFileUri != null,
        enter = AnimationUtils.dialogEnterAnimation,
        exit = AnimationUtils.dialogExitAnimation
    ) {
        if (selectedFileUri != null) {
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
    }

    // Delete folder dialog
    AnimatedVisibility(
        visible = showDeleteFolderDialog != null,
        enter = AnimationUtils.dialogEnterAnimation,
        exit = AnimationUtils.dialogExitAnimation
    ) {
        showDeleteFolderDialog?.let { folder ->
            AlertDialog(
                onDismissRequest = { showDeleteFolderDialog = null },
                title = { Text("Xóa thư mục") },
                text = { Text("Bạn có chắc chắn muốn xóa thư mục '${folder.name}'?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteFolder(folder.id)
                            showDeleteFolderDialog = null
                        }
                    ) {
                        Text("Xóa")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteFolderDialog = null }) {
                        Text("Hủy")
                    }
                }
            )
        }
    }

    // Delete document dialog
    AnimatedVisibility(
        visible = showDeleteDocumentDialog != null,
        enter = AnimationUtils.dialogEnterAnimation,
        exit = AnimationUtils.dialogExitAnimation
    ) {
        showDeleteDocumentDialog?.let { document ->
            AlertDialog(
                onDismissRequest = { showDeleteDocumentDialog = null },
                title = { Text("Xóa tài liệu") },
                text = { Text("Bạn có chắc chắn muốn xóa tài liệu '${document.name}'?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteDocument(document.id)
                            showDeleteDocumentDialog = null
                        }
                    ) {
                        Text("Xóa")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDocumentDialog = null }) {
                        Text("Hủy")
                    }
                }
            )
        }
    }
}
