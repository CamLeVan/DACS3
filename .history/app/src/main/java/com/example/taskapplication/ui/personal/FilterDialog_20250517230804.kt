package com.example.taskapplication.ui.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.taskapplication.R
import com.example.taskapplication.ui.components.DatePickerDialog
import com.example.taskapplication.ui.theme.HighPriority
import com.example.taskapplication.ui.theme.LowPriority
import com.example.taskapplication.ui.theme.MediumPriority
import com.example.taskapplication.util.formatDate
import java.util.Calendar

/**
 * Dialog cho phép lọc công việc theo nhiều tiêu chí
 * @param onDismiss Callback khi đóng dialog
 * @param onApplyFilter Callback khi áp dụng bộ lọc
 * @param onClearFilter Callback khi xóa bộ lọc
 * @param initialStatus Trạng thái ban đầu
 * @param initialPriority Độ ưu tiên ban đầu
 * @param initialDueDateStart Ngày bắt đầu ban đầu
 * @param initialDueDateEnd Ngày kết thúc ban đầu
 */
@Composable
fun FilterDialog(
    onDismiss: () -> Unit,
    onApplyFilter: (status: String?, priority: String?, dueDateStart: Long?, dueDateEnd: Long?) -> Unit,
    onClearFilter: () -> Unit,
    initialStatus: String? = null,
    initialPriority: String? = null,
    initialDueDateStart: Long? = null,
    initialDueDateEnd: Long? = null
) {
    var selectedStatus by remember { mutableStateOf(initialStatus) }
    var selectedPriority by remember { mutableStateOf(initialPriority) }
    var startDate by remember { mutableStateOf(initialDueDateStart) }
    var endDate by remember { mutableStateOf(initialDueDateEnd) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 6.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Tiêu đề
                Text(
                    text = stringResource(R.string.filter_tasks),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Lọc theo trạng thái
                Text(
                    text = stringResource(R.string.filter_by_status),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusFilterChip(
                        status = "pending",
                        isSelected = selectedStatus == "pending",
                        onSelected = { selectedStatus = if (selectedStatus == "pending") null else "pending" },
                        modifier = Modifier.weight(1f)
                    )

                    StatusFilterChip(
                        status = "in_progress",
                        isSelected = selectedStatus == "in_progress",
                        onSelected = { selectedStatus = if (selectedStatus == "in_progress") null else "in_progress" },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusFilterChip(
                        status = "completed",
                        isSelected = selectedStatus == "completed",
                        onSelected = { selectedStatus = if (selectedStatus == "completed") null else "completed" },
                        modifier = Modifier.weight(1f)
                    )

                    StatusFilterChip(
                        status = "overdue",
                        isSelected = selectedStatus == "overdue",
                        onSelected = { selectedStatus = if (selectedStatus == "overdue") null else "overdue" },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // Lọc theo độ ưu tiên
                Text(
                    text = stringResource(R.string.filter_by_priority),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PriorityFilterChip(
                        priority = "low",
                        isSelected = selectedPriority == "low",
                        onSelected = { selectedPriority = if (selectedPriority == "low") null else "low" },
                        modifier = Modifier.weight(1f)
                    )

                    PriorityFilterChip(
                        priority = "medium",
                        isSelected = selectedPriority == "medium",
                        onSelected = { selectedPriority = if (selectedPriority == "medium") null else "medium" },
                        modifier = Modifier.weight(1f)
                    )

                    PriorityFilterChip(
                        priority = "high",
                        isSelected = selectedPriority == "high",
                        onSelected = { selectedPriority = if (selectedPriority == "high") null else "high" },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // Lọc theo ngày hạn
                Text(
                    text = stringResource(R.string.filter_by_due_date),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Ngày bắt đầu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.start_date),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { showStartDatePicker = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = startDate?.let { formatDate(it) } ?: "Chọn ngày",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (startDate != null)
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Ngày kết thúc
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.end_date),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { showEndDatePicker = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = endDate?.let { formatDate(it) } ?: "Chọn ngày",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (endDate != null)
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Các nút hành động
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            selectedStatus = null
                            selectedPriority = null
                            startDate = null
                            endDate = null
                            onClearFilter()
                            onDismiss()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.clear_filters),
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = onDismiss
                    ) {
                        Text(text = stringResource(R.string.cancel))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            onApplyFilter(selectedStatus, selectedPriority, startDate, endDate)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(text = stringResource(R.string.apply_filters))
                    }
                }
            }
        }
    }

    // Date pickers
    if (showStartDatePicker) {
        DatePickerDialog(
            onDateSelected = {
                startDate = it
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false },
            initialDate = startDate ?: System.currentTimeMillis()
        )
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDateSelected = {
                // Đặt thời gian là cuối ngày
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = it
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                endDate = calendar.timeInMillis
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false },
            initialDate = endDate ?: System.currentTimeMillis()
        )
    }
}

/**
 * Chip lọc theo trạng thái
 */
@Composable
private fun StatusFilterChip(
    status: String,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        when (status) {
            "pending" -> MaterialTheme.colorScheme.primaryContainer
            "in_progress" -> MaterialTheme.colorScheme.secondaryContainer
            "completed" -> MaterialTheme.colorScheme.tertiaryContainer
            "overdue" -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.surface
        }
    } else {
        MaterialTheme.colorScheme.surface
    }

    val textColor = if (isSelected) {
        when (status) {
            "pending" -> MaterialTheme.colorScheme.primary
            "in_progress" -> MaterialTheme.colorScheme.secondary
            "completed" -> MaterialTheme.colorScheme.tertiary
            "overdue" -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurface
        }
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    }

    val statusText = when (status) {
        "pending" -> stringResource(R.string.status_pending)
        "in_progress" -> stringResource(R.string.status_in_progress)
        "completed" -> stringResource(R.string.status_completed)
        "overdue" -> stringResource(R.string.status_overdue)
        else -> status
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = if (isSelected) textColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onSelected() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )
    }
}

/**
 * Chip lọc theo độ ưu tiên
 */
@Composable
private fun PriorityFilterChip(
    priority: String,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val priorityColor = when (priority) {
        "low" -> LowPriority
        "medium" -> MediumPriority
        "high" -> HighPriority
        else -> Color.Gray
    }

    val backgroundColor = if (isSelected) {
        priorityColor.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val textColor = if (isSelected) {
        priorityColor
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    }

    val priorityText = when (priority) {
        "low" -> stringResource(R.string.priority_low)
        "medium" -> stringResource(R.string.priority_medium)
        "high" -> stringResource(R.string.priority_high)
        else -> priority
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = if (isSelected) priorityColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onSelected() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = priorityText,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )
    }
}