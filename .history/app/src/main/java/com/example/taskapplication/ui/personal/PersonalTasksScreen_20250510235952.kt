package com.example.taskapplication.ui.personal

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.taskapplication.R
import com.example.taskapplication.ui.animation.AnimationUtils
import com.example.taskapplication.ui.components.EmptyStateView
import com.example.taskapplication.ui.components.ErrorStateView
import com.example.taskapplication.ui.theme.ButtonGradientEnd
import com.example.taskapplication.ui.theme.ButtonGradientStart
import com.example.taskapplication.ui.theme.LocalExtendedColorScheme
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Màn hình hiển thị danh sách công việc cá nhân với giao diện hiện đại
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
    val extendedColorScheme = LocalExtendedColorScheme.current

    // Gradient cho TopAppBar
    val topAppBarGradient = Brush.horizontalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary
        )
    )

    // Gradient cho FloatingActionButton
    val fabGradient = Brush.linearGradient(
        colors = listOf(
            ButtonGradientStart,
            ButtonGradientEnd
        )
    )

    // State cho LazyColumn
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 5 }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.personal_tasks),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    // Nút đồng bộ với hiệu ứng
                    IconButton(
                        onClick = { viewModel.syncTasks() },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.sync),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            // FAB với gradient
            FloatingActionButton(
                onClick = { viewModel.showAddTaskDialog() },
                modifier = Modifier
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(fabGradient)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_task),
                    tint = Color.White
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            SwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing),
                onRefresh = { viewModel.syncTasks() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                val state = tasksState

                // Loading state with animation
                AnimatedVisibility(
                    visible = state is PersonalTasksViewModel.TasksState.Loading,
                    enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
                    exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Rotating animation for loading indicator
                            val rotation = rememberInfiniteTransition().animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1500, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "Loading Rotation"
                            )

                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(48.dp)
                                    .graphicsLayer { rotationZ = rotation.value }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Pulsating text animation
                            val scale = rememberInfiniteTransition().animateFloat(
                                initialValue = 0.95f,
                                targetValue = 1.05f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "Text Pulse"
                            )

                            Text(
                                text = "Đang tải công việc...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                            )
                        }
                    }
                }

                // Success state with empty view
                AnimatedVisibility(
                    visible = state is PersonalTasksViewModel.TasksState.Success && (state as? PersonalTasksViewModel.TasksState.Success)?.tasks?.isEmpty() == true,
                    enter = fadeIn(tween(500)) + expandIn(tween(500), expandFrom = Alignment.Center),
                    exit = fadeOut(tween(300)) + shrinkOut(tween(300), shrinkTowards = Alignment.Center)
                ) {
                    EmptyStateView(
                        message = stringResource(R.string.empty_tasks),
                        icon = Icons.Default.Info
                    )
                }

                // Success state with task list
                AnimatedVisibility(
                    visible = state is PersonalTasksViewModel.TasksState.Success && (state as? PersonalTasksViewModel.TasksState.Success)?.tasks?.isNotEmpty() == true,
                    enter = fadeIn(tween(500)),
                    exit = fadeOut(tween(300))
                ) {
                    if (state is PersonalTasksViewModel.TasksState.Success && state.tasks.isNotEmpty()) {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            itemsIndexed(
                                items = state.tasks,
                                key = { _, task -> task.id }
                            ) { index, task ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter = AnimationUtils.listItemEnterAnimation(index),
                                    exit = AnimationUtils.listItemExitAnimation
                                ) {
                                    TaskItem(
                                        task = task,
                                        onClick = { onTaskClick(task.id) },
                                        onCompleteClick = { viewModel.toggleTaskCompletion(task) }
                                    )
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }

                // Error state with animation
                AnimatedVisibility(
                    visible = state is PersonalTasksViewModel.TasksState.Error,
                    enter = fadeIn(tween(500)) + expandIn(tween(500), expandFrom = Alignment.Center),
                    exit = fadeOut(tween(300)) + shrinkOut(tween(300), shrinkTowards = Alignment.Center)
                ) {
                    if (state is PersonalTasksViewModel.TasksState.Error) {
                        ErrorStateView(
                            message = stringResource(R.string.error_loading_tasks, state.message),
                            onRetryClick = { viewModel.loadTasks() }
                        )
                    }
                }
            }

            // Nút cuộn lên đầu trang
            AnimatedVisibility(
                visible = showScrollToTop,
                enter = fadeIn() + slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 300)
                ),
                exit = fadeOut() + slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = 300)
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        // Cuộn lên đầu trang
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    modifier = Modifier
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        text = "Lên đầu trang",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }

    // Show add task dialog if necessary
    AnimatedVisibility(
        visible = showAddDialog,
        enter = AnimationUtils.dialogEnterAnimation,
        exit = AnimationUtils.dialogExitAnimation
    ) {
        AddTaskDialog(
            onDismiss = { viewModel.hideAddTaskDialog() },
            onTaskCreated = { task -> viewModel.createTask(task) }
        )
    }
}