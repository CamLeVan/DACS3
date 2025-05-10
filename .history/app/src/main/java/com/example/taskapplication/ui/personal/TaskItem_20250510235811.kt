package com.example.taskapplication.ui.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.taskapplication.domain.model.PersonalTask
import com.example.taskapplication.ui.components.GradientCard
import com.example.taskapplication.ui.theme.HighPriority
import com.example.taskapplication.ui.theme.LocalExtendedColorScheme
import com.example.taskapplication.ui.theme.LowPriority
import com.example.taskapplication.ui.theme.MediumPriority
import com.example.taskapplication.util.formatDate
import com.example.taskapplication.util.isOverdue

/**
 * Component hiển thị một công việc cá nhân với giao diện hiện đại
 * @param task Công việc cần hiển thị
 * @param onClick Callback khi nhấn vào công việc
 * @param onCompleteClick Callback khi nhấn vào checkbox hoàn thành
 */
@Composable
fun TaskItem(
    task: PersonalTask,
    onClick: () -> Unit,
    onCompleteClick: () -> Unit
) {
    val extendedColorScheme = LocalExtendedColorScheme.current

    // Xác định màu sắc dựa trên trạng thái công việc
    val syncStatusColor = when (task.syncStatus) {
        "synced" -> extendedColorScheme.taskCompleted
        "pending_create", "pending_update", "pending_delete" -> extendedColorScheme.taskPending
        else -> Color.Transparent
    }

    val isTaskOverdue = task.dueDate?.let { isOverdue(it) } ?: false
    val textColor = when {
        task.isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        isTaskOverdue -> extendedColorScheme.taskOverdue
        else -> MaterialTheme.colorScheme.onSurface
    }

    // Xác định màu ưu tiên
    val priorityColor = when (task.priority) {
        0 -> LowPriority
        1 -> MediumPriority
        2 -> HighPriority
        else -> MediumPriority
    }

    // Tạo gradient cho card dựa trên ưu tiên
    val cardGradient = if (task.isCompleted) {
        // Công việc đã hoàn thành - gradient nhạt
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            )
        )
    } else {
        // Công việc chưa hoàn thành - gradient dựa trên ưu tiên
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.surface,
                priorityColor.copy(alpha = 0.05f)
            )
        )
    }

    // Thêm hiệu ứng hover
    var isHovered by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "Card Scale Animation"
    )
    val elevation by animateDpAsState(
        targetValue = if (isHovered) (if (task.isCompleted) 2.dp else 5.dp) else (if (task.isCompleted) 1.dp else 3.dp),
        animationSpec = tween(durationMillis = 150),
        label = "Card Elevation Animation"
    )

    GradientCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        isHovered = event.type == PointerEventType.Enter || event.type == PointerEventType.Move
                    }
                }
            },
        onClick = onClick,
        cornerRadius = 12.dp,
        elevation = elevation,
        gradient = cardGradient
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox với màu tùy chỉnh
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onCompleteClick() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Hiển thị biểu tượng ưu tiên nếu là ưu tiên cao
                    if (task.priority == 2 && !task.isCompleted) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = HighPriority,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                task.description?.let { description ->
                    if (description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }
                }

                task.dueDate?.let { dueDate ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Hiển thị ngày với màu sắc dựa trên trạng thái
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isTaskOverdue && !task.isCompleted)
                                        extendedColorScheme.taskOverdue.copy(alpha = 0.1f)
                                    else
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isTaskOverdue && !task.isCompleted)
                                        extendedColorScheme.taskOverdue
                                    else
                                        MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formatDate(dueDate),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isTaskOverdue && !task.isCompleted)
                                        extendedColorScheme.taskOverdue
                                    else
                                        MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Sync status indicator với hiệu ứng đẹp hơn
            if (task.syncStatus != "synced") {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(syncStatusColor)
                )
            }
        }
    }
}
