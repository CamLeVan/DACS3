package com.example.taskapplication.ui.team.kanban

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.animateItemPlacement
import com.example.taskapplication.ui.team.kanban.components.CreateTaskDialog
import com.example.taskapplication.ui.team.kanban.components.KanbanColumn
import com.example.taskapplication.ui.team.kanban.components.TaskFilterDialog

/**
 * Screen that displays a kanban board for team tasks
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun KanbanBoardScreen(
    viewModel: KanbanViewModel = hiltViewModel(),
    teamId: String,
    onBackClick: () -> Unit,
    onTaskClick: (String) -> Unit
) {
    val kanbanState by viewModel.kanbanState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMenu by remember { mutableStateOf(false) }
    var showCreateTaskDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var currentColumnId by remember { mutableStateOf<String?>(null) }

    // Show error message in snackbar
    LaunchedEffect(kanbanState) {
        if (kanbanState is KanbanState.Error) {
            snackbarHostState.showSnackbar(
                message = (kanbanState as KanbanState.Error).message
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kanban Board") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter Tasks")
                    }

                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Create New Column") },
                            onClick = {
                                // TODO: Implement create column
                                showMenu = false
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Refresh Board") },
                            onClick = {
                                viewModel.loadKanbanBoard()
                                showMenu = false
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateTaskDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = kanbanState) {
                is KanbanState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is KanbanState.Empty -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = "No Kanban Board Found",
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Create a new board to get started",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                is KanbanState.Success -> {
                    val board = state.board

                    // Calculate board statistics
                    val totalTasks = board.columns.sumOf { it.tasks.size }
                    val completedTasks = board.columns.sumOf { column ->
                        column.tasks.count { it.priority == "LOW" } // Giả sử LOW = hoàn thành
                    }
                    val progress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.background,
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        // Header with board name and statistics
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = board.name,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "$totalTasks tasks • ${(progress * 100).toInt()}% completed",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                            }

                            // Add task button
                            Button(
                                onClick = {
                                    showCreateTaskDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Task",
                                    modifier = Modifier.size(16.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text("Add Task")
                            }
                        }

                        // Progress bar
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Board Progress",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                Text(
                                    text = "$completedTasks/$totalTasks",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }

                        // Columns
                        LazyRow(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(board.columns) { column ->
                                KanbanColumn(
                                    column = column,
                                    onTaskClick = onTaskClick,
                                    onTaskMove = { taskId, columnId, position ->
                                        viewModel.moveTask(taskId, columnId, position)
                                    },
                                    onAddTask = { columnId ->
                                        // Lưu columnId hiện tại và hiển thị dialog tạo nhiệm vụ
                                        currentColumnId = columnId
                                        showCreateTaskDialog = true
                                    },
                                    modifier = Modifier
                                        .width(300.dp)
                                )
                            }
                        }
                    }
                }

                is KanbanState.Error -> {
                    // Error is shown in snackbar, but we still need to show content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = "Failed to load Kanban Board",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }
            }
        }
    }

    if (showCreateTaskDialog) {
        CreateTaskDialog(
            onDismiss = {
                showCreateTaskDialog = false
                currentColumnId = null
            },
            onCreateTask = { title, description, dueDate, priority, assignedUserId, columnId ->
                viewModel.createTask(title, description, dueDate, priority, assignedUserId, columnId)
                showCreateTaskDialog = false
                currentColumnId = null
            },
            columns = if (kanbanState is KanbanState.Success) {
                (kanbanState as KanbanState.Success).board.columns
            } else {
                emptyList()
            },
            teamMembers = viewModel.teamMembers.collectAsState().value,
            initialColumnId = currentColumnId
        )
    }

    if (showFilterDialog) {
        TaskFilterDialog(
            onDismiss = { showFilterDialog = false },
            onApplyFilter = { assignedUserId, priority, isCompleted ->
                viewModel.applyFilter(assignedUserId, priority, isCompleted)
                showFilterDialog = false
            },
            teamMembers = viewModel.teamMembers.collectAsState().value,
            currentFilter = viewModel.currentFilter.collectAsState().value
        )
    }
}
