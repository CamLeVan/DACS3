package com.example.taskapplication.ui.team.kanban.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.taskapplication.domain.model.KanbanColumn
import com.example.taskapplication.domain.model.KanbanTask
import com.example.taskapplication.ui.theme.LocalExtendedColorScheme
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

/**
 * Component that displays a column in a kanban board
 */
@Composable
fun KanbanColumn(
    column: KanbanColumn,
    onTaskClick: (String) -> Unit,
    onTaskMove: (String, String, Int) -> Unit,
    onAddTask: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    // Calculate column statistics
    val completedTasks by remember(column.tasks) {
        derivedStateOf {
            column.tasks.count { it.priority == "LOW" } // Giả sử priority LOW là đã hoàn thành
        }
    }

    val progress by remember(column.tasks) {
        derivedStateOf {
            if (column.tasks.isEmpty()) 0f else completedTasks.toFloat() / column.tasks.size
        }
    }

    // Determine column color based on name
    val columnColor = when (column.name) {
        "To Do" -> LocalExtendedColorScheme.current.todoColumn
        "In Progress" -> LocalExtendedColorScheme.current.inProgressColumn
        "Done" -> LocalExtendedColorScheme.current.doneColumn
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    // Animated elevation for better visual feedback
    val elevation by animateDpAsState(
        targetValue = if (showMenu) 8.dp else 4.dp,
        animationSpec = tween(durationMillis = 300),
        label = "Card Elevation Animation"
    )

    Card(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(16.dp),
                spotColor = columnColor.copy(alpha = 0.5f)
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = columnColor.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(12.dp)
        ) {
            // Column header with colored accent
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Colored circle indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(columnColor)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = column.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.weight(1f)
                )

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Column Options",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add Task") },
                            onClick = {
                                onAddTask(column.id)
                                showMenu = false
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Edit Column") },
                            onClick = {
                                // TODO: Implement edit column
                                showMenu = false
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Delete Column") },
                            onClick = {
                                // TODO: Implement delete column
                                showMenu = false
                            }
                        )
                    }
                }
            }

            // Progress indicator and task count
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${column.tasks.size} tasks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = columnColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Tasks list with drag-and-drop
            if (column.tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tasks in this column",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val state = rememberReorderableLazyListState(
                    onMove = { from, to ->
                        // Handle move within the same column
                        val fromTask = column.tasks[from.index]
                        onTaskMove(fromTask.id, column.id, to.index)
                    },
                    onDragEnd = { _, _ -> }
                )

                LazyColumn(
                    state = state.listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .reorderable(state)
                        .detectReorderAfterLongPress(state)
                ) {
                    items(column.tasks, { it.id }) { task ->
                        ReorderableItem(state, key = task.id) { isDragging ->
                            KanbanTaskCard(
                                task = task,
                                onClick = { onTaskClick(task.id) },
                                isDragging = isDragging
                            )
                        }
                    }
                }
            }

            // Add task button - enhanced with gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                columnColor.copy(alpha = 0.1f),
                                columnColor.copy(alpha = 0.2f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = columnColor.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { onAddTask(column.id) },
                    modifier = Modifier.padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Task",
                        tint = columnColor
                    )
                }
            }
        }
    }
}
