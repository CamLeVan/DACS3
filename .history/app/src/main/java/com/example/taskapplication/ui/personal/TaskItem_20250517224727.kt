package com.example.taskapplication.ui.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.taskapplication.domain.model.PersonalTask
import com.example.taskapplication.ui.components.GradientBorderCard
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
    onCompleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extendedColorScheme = LocalExtendedColorScheme.current

    // Xác định màu sắc dựa trên trạng thái công việc
    val syncStatusColor = when (task.syncStatus) {
        PersonalTask.SyncStatus.SYNCED -> extendedColorScheme.taskCompleted
        PersonalTask.SyncStatus.CREATED,
        PersonalTask.SyncStatus.UPDATED,
        PersonalTask.SyncStatus.DELETED -> extendedColorScheme.taskPending
        else -> Color.Transparent
    }

    val isTaskOverdue = task.dueDate?.let { it < System.currentTimeMillis() } ?: false
    val textColor = when {
        task.status == "completed" -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        isTaskOverdue -> extendedColorScheme.taskOverdue
        else -> MaterialTheme.colorScheme.onSurface
    }

    // Xác định màu ưu tiên
    val priorityColor = when (task.priority) {
        "low" -> LowPriority
        "medium" -> MediumPriority
        "high" -> HighPriority
        else -> MediumPriority
    }

    // Tạo gradient cho card dựa trên ưu tiên
    val cardBorderGradient = if (task.status == "completed") {
        // Công việc đã hoàn thành - gradient nhạt
        Brush.horizontalGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        )
    } else {
        // Công việc chưa hoàn thành - gradient dựa trên ưu tiên
        Brush.horizontalGradient(
            listOf(
                priorityColor.copy(alpha = 0.5f),
                priorityColor.copy(alpha = 0.2f)
            )
        )
    }

    // Hiệu ứng scale khi hover
    var isHovered by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.01f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "Card Scale Animation"
    )

    // Sử dụng GradientBorderCard cho giao diện hiện đại hơn
    GradientBorderCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .scale(scale),
        onClick = onClick,
        cornerRadius = 12.dp,
        elevation = if (task.status == "completed") 1.dp else 2.dp,
        borderWidth = 1.dp,
        gradient = cardBorderGradient,
        backgroundColor = if (task.status == "completed")
            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        else
            MaterialTheme.colorScheme.surface,
        contentPadding = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox hiện đại hơn với icon
            IconButton(
                onClick = { onCompleteClick() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (task.status == "completed")
                        Icons.Rounded.CheckCircle
                    else
                        Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (task.status == "completed")
                        extendedColorScheme.taskCompleted
                    else
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (task.status == "completed") FontWeight.Normal else FontWeight.Medium
                        ),
                        color = textColor,
                        textDecoration = if (task.status == "completed") TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Hiển thị biểu tượng ưu tiên nếu là ưu tiên cao
                    if (task.priority == "high" && task.status != "completed") {
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
                            textDecoration = if (task.status == "completed") TextDecoration.LineThrough else TextDecoration.None
                        )
                    }
                }

                task.dueDate?.let { dueDate ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Hiển thị ngày với thiết kế hiện đại hơn
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isTaskOverdue && task.status != "completed")
                                        extendedColorScheme.taskOverdue.copy(alpha = 0.1f)
                                    else
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isTaskOverdue && task.status != "completed")
                                        extendedColorScheme.taskOverdue.copy(alpha = 0.2f)
                                    else
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isTaskOverdue && task.status != "completed")
                                        extendedColorScheme.taskOverdue
                                    else
                                        MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formatDate(dueDate),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = if (isTaskOverdue && task.status != "completed")
                                        extendedColorScheme.taskOverdue
                                    else
                                        MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Sync status indicator với thiết kế hiện đại hơn
            if (task.syncStatus != "synced") {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(syncStatusColor)
                        .border(
                            width = 1.dp,
                            color = syncStatusColor.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}
