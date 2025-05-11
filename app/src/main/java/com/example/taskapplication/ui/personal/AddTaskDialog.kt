package com.example.taskapplication.ui.personal

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.taskapplication.R
import com.example.taskapplication.domain.model.PersonalTask
import com.example.taskapplication.ui.components.GradientButton
import com.example.taskapplication.ui.theme.HighPriority
import com.example.taskapplication.ui.theme.LocalExtendedColorScheme
import com.example.taskapplication.ui.theme.LowPriority
import com.example.taskapplication.ui.theme.MediumPriority
import com.example.taskapplication.util.formatDate
import com.example.taskapplication.util.formatDateWithTime
import java.util.Calendar
import java.util.UUID
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

/**
 * Dialog hiện đại để thêm hoặc chỉnh sửa công việc
 * @param onDismiss Callback khi đóng dialog
 * @param onTaskCreated Callback khi tạo công việc mới
 * @param existingTask Công việc hiện tại (nếu là chỉnh sửa)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onTaskCreated: (PersonalTask) -> Unit,
    existingTask: PersonalTask? = null
) {
    val isEditMode = existingTask != null
    val title = stringResource(if (isEditMode) R.string.edit_task else R.string.add_task)
    val extendedColorScheme = LocalExtendedColorScheme.current

    var taskTitle by remember { mutableStateOf(existingTask?.title ?: "") }
    var taskDescription by remember { mutableStateOf(existingTask?.description ?: "") }
    var taskDueDate by remember { mutableStateOf(existingTask?.dueDate) }
    var taskPriority by remember { mutableStateOf(existingTask?.priority ?: 1) } // Default: Medium
    var isExpanded by remember { mutableStateOf(false) }
    var taskLabels by remember { mutableStateOf(existingTask?.labels ?: emptyList()) }
    var labelInput by remember { mutableStateOf("") }
    val labelSuggestions = listOf("Cá nhân", "Khẩn cấp", "Học tập", "Gia đình", "Công việc", "Sức khỏe", "Mua sắm")
    val filteredSuggestions = labelSuggestions.filter {
        it.contains(labelInput, ignoreCase = true) && !taskLabels.contains(it)
    }.take(5)

    var reminderMinutesBefore by remember { mutableStateOf(existingTask?.reminderMinutesBefore) }
    var showCustomInput by remember { mutableStateOf(false) }
    var customReminderInput by remember { mutableStateOf("") }
    val reminderOptions = listOf(
        null to stringResource(R.string.reminder_none),
        5 to stringResource(R.string.reminder_5min),
        10 to stringResource(R.string.reminder_10min),
        30 to stringResource(R.string.reminder_30min),
        60 to stringResource(R.string.reminder_1hour),
        1440 to stringResource(R.string.reminder_1day),
        -1 to stringResource(R.string.reminder_custom)
    )
    val lemonYellow = Color(0xFFFFEB3B) // Màu vàng chanh

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    taskDueDate?.let {
        calendar.timeInMillis = it
    }

    var showConfirmDialog by remember { mutableStateOf(false) }

    // DateTimePicker hiện đại
    fun showDateTimePicker(onDateTimeSelected: (Long) -> Unit) {
        val now = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val pickedDate = Calendar.getInstance()
                pickedDate.set(Calendar.YEAR, year)
                pickedDate.set(Calendar.MONTH, month)
                pickedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                // Sau khi chọn ngày, show tiếp TimePicker
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        pickedDate.set(Calendar.HOUR_OF_DAY, hour)
                        pickedDate.set(Calendar.MINUTE, minute)
                        pickedDate.set(Calendar.SECOND, 0)
                        pickedDate.set(Calendar.MILLISECOND, 0)
                        onDateTimeSelected(pickedDate.timeInMillis)
                    },
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE),
                    true
                ).show()
            },
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    // Xác định màu ưu tiên
    val priorityColor = when (taskPriority) {
        0 -> LowPriority
        1 -> MediumPriority
        2 -> HighPriority
        else -> MediumPriority
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Tiêu đề với gradient
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Title field
                OutlinedTextField(
                    value = taskTitle,
                    onValueChange = { taskTitle = it },
                    label = { Text(stringResource(R.string.task_title)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Description field
                OutlinedTextField(
                    value = taskDescription,
                    onValueChange = { taskDescription = it },
                    label = { Text(stringResource(R.string.task_description)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Due date field -
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            showDateTimePicker { selectedMillis ->
                                taskDueDate = selectedMillis
                            }
                        }
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = stringResource(R.string.task_due_date),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = taskDueDate?.let { formatDateWithTime(it) } ?: stringResource(R.string.select_date),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (taskDueDate == null)
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                else
                                    MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Priority selector - Thiết kế hiện đại hơn
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.task_priority),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Low priority
                            PriorityOption(
                                text = stringResource(R.string.priority_low),
                                color = LowPriority,
                                isSelected = taskPriority == 0,
                                onClick = { taskPriority = 0 },
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Medium priority
                            PriorityOption(
                                text = stringResource(R.string.priority_medium),
                                color = MediumPriority,
                                isSelected = taskPriority == 1,
                                onClick = { taskPriority = 1 },
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // High priority
                            PriorityOption(
                                text = stringResource(R.string.priority_high),
                                color = HighPriority,
                                isSelected = taskPriority == 2,
                                onClick = { taskPriority = 2 },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // UI chọn nhãn (labels)
                Text(
                    text = stringResource(R.string.task_labels),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = labelInput,
                    onValueChange = { labelInput = it },
                    label = { Text(stringResource(R.string.add_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    trailingIcon = {
                        if (labelInput.isNotBlank()) {
                            IconButton(onClick = {
                                val trimmed = labelInput.trim()
                                if (trimmed.isNotEmpty() && !taskLabels.contains(trimmed)) {
                                    taskLabels = taskLabels + trimmed
                                    labelInput = ""
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = stringResource(R.string.add_label)
                                )
                            }
                        }
                    }
                )
                // Gợi ý nhãn
                if (filteredSuggestions.isNotEmpty() && labelInput.isNotBlank()) {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        filteredSuggestions.forEach { suggestion ->
                            AssistChip(
                                onClick = {
                                    taskLabels = taskLabels + suggestion
                                    labelInput = ""
                                },
                                label = { Text(suggestion) },
                                modifier = Modifier.padding(end = 8.dp),
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                )
                            )
                        }
                    }
                }
                // Hiển thị nhãn đã chọn
                if (taskLabels.isNotEmpty()) {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        taskLabels.forEach { label ->
                            AssistChip(
                                onClick = {},
                                label = { Text(label) },
                                trailingIcon = {
                                    IconButton(onClick = { taskLabels = taskLabels - label }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.remove_label)
                                        )
                                    }
                                },
                                modifier = Modifier.padding(end = 8.dp),
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Reminder selector UI
                Text(
                    text = stringResource(R.string.task_reminder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    reminderOptions.forEach { (value, label) ->
                        val isSelected = (reminderMinutesBefore == value && !showCustomInput) || (value == -1 && showCustomInput)
                        AssistChip(
                            onClick = {
                                if (value == -1) {
                                    showCustomInput = true
                                    customReminderInput = if (reminderMinutesBefore != null && reminderMinutesBefore != 0) reminderMinutesBefore.toString() else ""
                                    reminderMinutesBefore = null
                                } else {
                                    showCustomInput = false
                                    reminderMinutesBefore = value
                                }
                            },
                            label = { Text(label) },
                            modifier = Modifier.padding(end = 8.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isSelected) lemonYellow else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
                if (showCustomInput) {
                    OutlinedTextField(
                        value = customReminderInput,
                        onValueChange = {
                            customReminderInput = it.filter { c -> c.isDigit() }
                            reminderMinutesBefore = customReminderInput.toIntOrNull()
                        },
                        label = { Text(stringResource(R.string.reminder_custom_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = lemonYellow,
                            unfocusedBorderColor = lemonYellow.copy(alpha = 0.5f),
                            cursorColor = lemonYellow
                        )
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                Spacer(modifier = Modifier.height(32.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel button
                    GradientButton(
                        text = stringResource(R.string.cancel),
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        gradientColors = listOf(
                            Color.Gray.copy(alpha = 0.5f),
                            Color.Gray.copy(alpha = 0.7f)
                        )
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Save button
                    GradientButton(
                        text = stringResource(R.string.save),
                        onClick = {
                            if (taskTitle.isNotEmpty()) {
                                showConfirmDialog = true
                            }
                        },
                        enabled = taskTitle.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Popup xác nhận lưu task
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(stringResource(R.string.confirm_save_title)) },
            text = { Text(stringResource(R.string.confirm_save_message)) },
            confirmButton = {
                TextButton(onClick = {
                    val task = if (isEditMode) {
                        existingTask!!.copy(
                            title = taskTitle,
                            description = taskDescription.ifEmpty { null },
                            dueDate = taskDueDate,
                            priority = taskPriority,
                            syncStatus = "pending_update",
                            lastModified = System.currentTimeMillis(),
                            labels = taskLabels,
                            reminderMinutesBefore = reminderMinutesBefore
                        )
                    } else {
                        PersonalTask(
                            id = UUID.randomUUID().toString(),
                            title = taskTitle,
                            description = taskDescription.ifEmpty { null },
                            dueDate = taskDueDate,
                            priority = taskPriority,
                            isCompleted = false,
                            syncStatus = "pending_create",
                            lastModified = System.currentTimeMillis(),
                            createdAt = System.currentTimeMillis(),
                            labels = taskLabels,
                            reminderMinutesBefore = reminderMinutesBefore
                        )
                    }
                    onTaskCreated(task)
                    onDismiss()
                    showConfirmDialog = false
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

/**
 * Tùy chọn ưu tiên cho công việc
 */
@Composable
private fun PriorityOption(
    text: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent
            )
            .border(
                width = 1.dp,
                color = if (isSelected) color else Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
