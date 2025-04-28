package com.example.taskapplication.ui.personal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.taskapplication.R
import com.example.taskapplication.ui.components.EmptyStateView
import com.example.taskapplication.ui.components.ErrorStateView
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

/**
 * Màn hình hiển thị danh sách công việc cá nhân
 * @param viewModel ViewModel quản lý trạng thái và logic
 * @param onTaskClick Callback khi nhấn vào một công việc
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalTasksScreen(
    viewModel: PersonalTasksViewModel = hiltViewModel(),
    onTaskClick: (String) -> Unit
) {
    val tasksState by viewModel.tasks.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.personal_tasks)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { viewModel.syncTasks() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.sync)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddTaskDialog() }) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_task)
                )
            }
        }
    ) { paddingValues ->
        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing),
            onRefresh = { viewModel.syncTasks() },
            modifier = Modifier.padding(paddingValues)
        ) {
            when (val state = tasksState) {
                is PersonalTasksViewModel.TasksState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
                is PersonalTasksViewModel.TasksState.Success -> {
                    if (state.tasks.isEmpty()) {
                        EmptyStateView(
                            message = stringResource(R.string.empty_tasks),
                            icon = Icons.Default.Info
                        )
                    } else {
                        LazyColumn {
                            items(state.tasks) { task ->
                                TaskItem(
                                    task = task,
                                    onClick = { onTaskClick(task.id) },
                                    onCompleteClick = { viewModel.toggleTaskCompletion(task) }
                                )
                            }
                        }
                    }
                }
                is PersonalTasksViewModel.TasksState.Error -> {
                    ErrorStateView(
                        message = stringResource(R.string.error_loading_tasks, state.message),
                        onRetryClick = { viewModel.loadTasks() }
                    )
                }
            }
        }
    }

    // Show add task dialog if necessary
    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { viewModel.hideAddTaskDialog() },
            onTaskCreated = { task -> viewModel.createTask(task) }
        )
    }
}