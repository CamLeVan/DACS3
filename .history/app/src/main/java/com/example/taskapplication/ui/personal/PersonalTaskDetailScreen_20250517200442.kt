package com.example.taskapplication.ui.personal

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.taskapplication.R
import com.example.taskapplication.domain.model.PersonalTask
import com.example.taskapplication.ui.components.ErrorStateView
import com.example.taskapplication.util.formatDate
import com.example.taskapplication.util.isOverdue

/**
 * Màn hình chi tiết công việc cá nhân
 * @param taskId ID của công việc cần hiển thị
 * @param onBackClick Callback khi nhấn nút quay lại
 * @param viewModel ViewModel quản lý trạng thái và logic
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalTaskDetailScreen(
    taskId: String,
    onBackClick: () -> Unit,
    viewModel: PersonalTaskDetailViewModel = hiltViewModel()
) {
    var task by remember { mutableStateOf<PersonalTask?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(taskId) {
        isLoading = true
        error = null
        try {
            val result = viewModel.getTask(taskId)
            task = result
            isLoading = false
        } catch (e: Exception) {
            error = e.message
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.task_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showEditDialog = true },
                        enabled = task != null
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit_task)
                        )
                    }
                    IconButton(
                        onClick = {
                            task?.let {
                                viewModel.deleteTask(it.id)
                                onBackClick()
                            }
                        },
                        enabled = task != null
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete"
                        )
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
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null -> {
                    ErrorStateView(
                        message = stringResource(R.string.error_loading_tasks, error ?: ""),
                        onRetryClick = {
                            // Reload task
                            isLoading = true
                            error = null
                            viewModel.loadTask(taskId)
                        }
                    )
                }
                task != null -> {
                    val currentTask = task!!
                    val isTaskOverdue = currentTask.dueDate?.let { isOverdue(it) } ?: false
                    val textColor = when {
                        currentTask.isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        isTaskOverdue -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Title and completion status
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = currentTask.isCompleted,
                                onCheckedChange = {
                                    viewModel.toggleTaskCompletion(currentTask)
                                    task = currentTask.copy(isCompleted = !currentTask.isCompleted)
                                }
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = currentTask.title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = textColor,
                                textDecoration = if (currentTask.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Task details card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Due date
                                currentTask.dueDate?.let { dueDate ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.DateRange,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = if (isTaskOverdue && !currentTask.isCompleted)
                                                MaterialTheme.colorScheme.error
                                            else
                                                MaterialTheme.colorScheme.primary
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column {
                                            Text(
                                                text = stringResource(R.string.task_due_date),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            )

                                            Text(
                                                text = formatDate(dueDate),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = if (isTaskOverdue && !currentTask.isCompleted)
                                                    MaterialTheme.colorScheme.error
                                                else
                                                    MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    Divider()
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                // Priority
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = when (currentTask.priority) {
                                            0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) // Low
                                            1 -> MaterialTheme.colorScheme.primary // Medium
                                            2 -> MaterialTheme.colorScheme.error // High
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column {
                                        Text(
                                            text = stringResource(R.string.task_priority),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )

                                        Text(
                                            text = when (currentTask.priority) {
                                                0 -> stringResource(R.string.priority_low)
                                                1 -> stringResource(R.string.priority_medium)
                                                2 -> stringResource(R.string.priority_high)
                                                else -> stringResource(R.string.priority_medium)
                                            },
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                if (currentTask.description != null && currentTask.description.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Divider()
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Description
                                    Text(
                                        text = stringResource(R.string.task_description),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = currentTask.description,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = textColor,
                                        textDecoration = if (currentTask.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Divider()
                                Spacer(modifier = Modifier.height(16.dp))

                                // Subtasks
                                Text(
                                    text = "Công việc con",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Load subtasks
                                LaunchedEffect(currentTask.id) {
                                    viewModel.loadSubtasks(currentTask.id)
                                }

                                val subtasks by viewModel.subtasks.collectAsState()
                                val isLoadingSubtasks by viewModel.isLoadingSubtasks.collectAsState()

                                if (isLoadingSubtasks) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .align(Alignment.CenterHorizontally)
                                    )
                                } else if (subtasks.isEmpty()) {
                                    Text(
                                        text = "Chưa có công việc con nào",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                } else {
                                    Column {
                                        subtasks.forEach { subtask ->
                                            SubtaskItem(
                                                subtask = subtask,
                                                onToggleCompletion = { viewModel.toggleSubtaskCompletion(subtask) },
                                                onDelete = { viewModel.deleteSubtask(subtask.id) }
                                            )
                                        }
                                    }
                                }

                                // Add subtask button
                                var showAddSubtaskDialog by remember { mutableStateOf(false) }

                                Button(
                                    onClick = { showAddSubtaskDialog = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Thêm công việc con")
                                }

                                if (showAddSubtaskDialog) {
                                    var subtaskTitle by remember { mutableStateOf("") }

                                    AlertDialog(
                                        onDismissRequest = { showAddSubtaskDialog = false },
                                        title = { Text("Thêm công việc con") },
                                        text = {
                                            OutlinedTextField(
                                                value = subtaskTitle,
                                                onValueChange = { subtaskTitle = it },
                                                label = { Text("Tiêu đề") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        },
                                        confirmButton = {
                                            Button(
                                                onClick = {
                                                    if (subtaskTitle.isNotBlank()) {
                                                        viewModel.createSubtask(currentTask.id, subtaskTitle)
                                                        showAddSubtaskDialog = false
                                                    }
                                                }
                                            ) {
                                                Text("Thêm")
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(
                                                onClick = { showAddSubtaskDialog = false }
                                            ) {
                                                Text("Hủy")
                                            }
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Divider()
                                Spacer(modifier = Modifier.height(16.dp))

                                // Sync status
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.sync_status),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = if (currentTask.syncStatus == PersonalTask.SyncStatus.SYNCED)
                                            stringResource(R.string.synced)
                                        else
                                            stringResource(R.string.pending),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (currentTask.syncStatus == PersonalTask.SyncStatus.SYNCED)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            Color(0xFFFFA000) // Amber
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Show edit dialog if necessary
    if (showEditDialog && task != null) {
        AddTaskDialog(
            onDismiss = { showEditDialog = false },
            onTaskCreated = { updatedTask ->
                viewModel.updateTask(updatedTask)
                task = updatedTask
                showEditDialog = false
            },
            existingTask = task
        )
    }
}
