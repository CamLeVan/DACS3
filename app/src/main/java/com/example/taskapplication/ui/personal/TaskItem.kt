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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontWeight

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

    // Sửa lại logic quá hạn: chỉ task chưa hoàn thành và đã qua hạn mới là quá hạn
    val isTaskOverdue = !task.isCompleted && task.dueDate?.let { isOverdue(it) } == true
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

    // Nền theo trạng thái
    val backgroundColor = when {
        task.isCompleted -> Color(0xFFE8F5E9) // xanh nhạt
        isTaskOverdue -> Color(0xFFFFF3E0) // cam nhạt
        else -> Color(0xFFF3F1F8) // tím nhạt
    }
    // Viền nhạt
    val borderColor = when {
        task.isCompleted -> Color(0xFFB2DFDB)
        isTaskOverdue -> Color(0xFFFFCCBC)
        else -> Color(0xFFE0E0E0)
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 0.dp,
        border = BorderStroke(2.dp, borderColor),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon trạng thái
            Icon(
                imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (task.isCompleted) Color(0xFF4CAF50) else Color(0xFF6A30CF),
                modifier = Modifier.size(28.dp).clickable { onCompleteClick() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Chip trạng thái
                    if (task.isCompleted) {
                        ChipText(text = "Hoàn thành", color = Color(0xFF4CAF50), bg = Color(0xFFB2DFDB))
                    } else if (isTaskOverdue) {
                        ChipText(text = "Quá hạn", color = Color(0xFFF44336), bg = Color(0xFFFFCDD2))
                    } else {
                        ChipText(text = "Đang chờ", color = Color(0xFF1976D2), bg = Color(0xFFBBDEFB))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Chip độ ưu tiên
                    when (task.priority) {
                        2 -> ChipText(text = "Cao", color = Color(0xFFF44336), bg = Color(0xFFFFCDD2))
                        1 -> ChipText(text = "Trung bình", color = Color(0xFFFFA000), bg = Color(0xFFFFECB3))
                        else -> {}
                    }
                    if (task.dueDate != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = Color(0xFF757575),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = formatDate(task.dueDate),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF757575),
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
                if (!task.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF616161),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ChipText(text: String, color: Color, bg: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
        )
    }
}
