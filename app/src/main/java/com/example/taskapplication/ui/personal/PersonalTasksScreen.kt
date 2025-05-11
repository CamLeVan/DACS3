package com.example.taskapplication.ui.personal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.taskapplication.R
import com.example.taskapplication.ui.components.EmptyStateView
import com.example.taskapplication.ui.components.ErrorStateView
import com.example.taskapplication.ui.theme.ButtonGradientEnd
import com.example.taskapplication.ui.theme.ButtonGradientStart
import com.example.taskapplication.ui.theme.LocalExtendedColorScheme
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues

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

    // Màu tím đậm chủ đạo
    val deepPurple = Color(0xFF6A30CF)
    // State filter
    val filterOptions = listOf("Tất cả", "Quá hạn", "Đang chờ", "Hoàn thành")
    var selectedFilter by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            // Chỉ còn thanh tiêu đề
            Box(
    modifier = Modifier
        .fillMaxWidth()
        .background(deepPurple)
        .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.List,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.personal_tasks),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = { viewModel.syncTasks() },
            modifier = Modifier
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.15f), CircleShape)
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = stringResource(R.string.sync),
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
        },
        floatingActionButton = {
            // FAB tím đậm
            FloatingActionButton(
                onClick = { viewModel.showAddTaskDialog() },
                containerColor = deepPurple,
                contentColor = Color.White,
                modifier = Modifier.shadow(8.dp, CircleShape)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_task),
                    tint = Color.White
                )
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Filter bar hiện đại
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .shadow(2.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 14.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                filterOptions.forEachIndexed { idx, label ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selectedFilter == idx) deepPurple else Color(0xFFF3F1F8))
                            .padding(horizontal = 24.dp, vertical = 10.dp)
                            .clickable { selectedFilter = idx },
                    ) {
                        Text(
                            text = label,
                            color = if (selectedFilter == idx) Color.White else Color(0xFF6A30CF),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
            androidx.compose.material.Divider(color = Color(0xFFF3F1F8), thickness = 2.dp)
            Box(modifier = Modifier.fillMaxSize()) {
                SwipeRefresh(
                    state = rememberSwipeRefreshState(isRefreshing),
                    onRefresh = { viewModel.syncTasks() },
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    when (val state = tasksState) {
                        is PersonalTasksViewModel.TasksState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(48.dp)
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "Đang tải công việc...",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                        is PersonalTasksViewModel.TasksState.Success -> {
                            if (state.tasks.isEmpty()) {
                                // Hiển thị trạng thái trống với animation
                                EmptyStateView(
                                    message = stringResource(R.string.empty_tasks),
                                    icon = Icons.Default.Info
                                )
                            } else {
                                // Hiển thị danh sách công việc với animation
                                LazyColumn(
                                    state = listState,
                                    contentPadding = PaddingValues(bottom = 80.dp)
                                ) {
                                    item {
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }

                                    items(
                                        items = state.tasks,
                                        key = { it.id }
                                    ) { task ->
                                        TaskItem(
                                            task = task,
                                            onClick = { onTaskClick(task.id) },
                                            onCompleteClick = { viewModel.toggleTaskCompletion(task) }
                                        )
                                    }

                                    item {
                                        Spacer(modifier = Modifier.height(16.dp))
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

                // Nút cuộn lên đầu trang
                androidx.compose.animation.AnimatedVisibility(
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
    }

    // Show add task dialog if necessary
    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { viewModel.hideAddTaskDialog() },
            onTaskCreated = { task -> viewModel.createTask(task) }
        )
    }
}