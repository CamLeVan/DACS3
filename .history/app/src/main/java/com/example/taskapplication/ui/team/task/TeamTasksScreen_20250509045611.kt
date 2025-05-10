package com.example.taskapplication.ui.team.task

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.taskapplication.ui.team.kanban.KanbanBoardScreen
import com.example.taskapplication.ui.team.task.components.CreateTaskDialog
import com.example.taskapplication.ui.team.task.components.TeamTaskItem

/**
 * Screen that displays a list of tasks for a team
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamTasksScreen(
    viewModel: TeamTaskViewModel = hiltViewModel(),
    teamId: String,
    onBackClick: () -> Unit,
    onTaskClick: (String) -> Unit
) {
    val tasksState by viewModel.tasksState.collectAsState()
    val createTaskState by viewModel.createTaskState.collectAsState()
    val showCreateTaskDialog by viewModel.showCreateTaskDialog.collectAsState()
    val teamMembersState by viewModel.teamMembersState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Show error message in snackbar
    LaunchedEffect(tasksState) {
        if (tasksState is TeamTasksState.Error) {
            snackbarHostState.showSnackbar(
                message = (tasksState as TeamTasksState.Error).message
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Team Tasks") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showCreateTaskDialog() }) {
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
            when (tasksState) {
                is TeamTasksState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is TeamTasksState.Empty -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No tasks yet",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(onClick = { viewModel.showCreateTaskDialog() }) {
                            Text("Create a task")
                        }
                    }
                }

                is TeamTasksState.Success -> {
                    val tasks = (tasksState as TeamTasksState.Success).tasks
                    LazyColumn {
                        items(tasks) { task ->
                            TeamTaskItem(
                                task = task,
                                onClick = { onTaskClick(task.id) },
                                onToggleCompletion = { viewModel.toggleTaskCompletion(task) }
                            )
                        }
                    }
                }

                is TeamTasksState.Error -> {
                    // Error is shown in snackbar, but we still need to show content
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Failed to load tasks",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(onClick = { viewModel.loadTasks() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }

    // Show create task dialog
    if (showCreateTaskDialog) {
        CreateTaskDialog(
            createTaskState = createTaskState,
            teamMembersState = teamMembersState,
            onDismiss = { viewModel.hideCreateTaskDialog() },
            onCreateTask = { title, description, dueDate, priority, assignedUserId ->
                viewModel.createTask(title, description, dueDate, priority, assignedUserId)
            }
        )
    }
}
