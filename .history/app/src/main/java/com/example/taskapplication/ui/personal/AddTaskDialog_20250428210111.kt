package com.example.taskapplication.ui.personal

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.taskapplication.R
import com.example.taskapplication.domain.model.PersonalTask
import com.example.taskapplication.util.formatDate
import java.util.Calendar
import java.util.UUID

/**
 * Dialog để thêm hoặc chỉnh sửa công việc
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

    var taskTitle by remember { mutableStateOf(existingTask?.title ?: "") }
    var taskDescription by remember { mutableStateOf(existingTask?.description ?: "") }
    var taskDueDate by remember { mutableStateOf(existingTask?.dueDate) }
    var taskPriority by remember { mutableStateOf(existingTask?.priority ?: 1) } // Default: Medium
    var isExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    taskDueDate?.let {
        calendar.timeInMillis = it
    }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            taskDueDate = calendar.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title field
                OutlinedTextField(
                    value = taskTitle,
                    onValueChange = { taskTitle = it },
                    label = { Text(stringResource(R.string.task_title)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Description field
                OutlinedTextField(
                    value = taskDescription,
                    onValueChange = { taskDescription = it },
                    label = { Text(stringResource(R.string.task_description)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Due date field
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { datePickerDialog.show() }
                        .padding(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = stringResource(R.string.task_due_date),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = taskDueDate?.let { formatDate(it) } ?: stringResource(R.string.select_date),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (taskDueDate == null) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                               else MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Priority dropdown
                ExposedDropdownMenuBox(
                    expanded = isExpanded,
                    onExpandedChange = { isExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = when (taskPriority) {
                            0 -> stringResource(R.string.priority_low)
                            1 -> stringResource(R.string.priority_medium)
                            2 -> stringResource(R.string.priority_high)
                            else -> stringResource(R.string.priority_medium)
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.task_priority)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = isExpanded,
                        onDismissRequest = { isExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.priority_low)) },
                            onClick = {
                                taskPriority = 0
                                isExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.priority_medium)) },
                            onClick = {
                                taskPriority = 1
                                isExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.priority_high)) },
                            onClick = {
                                taskPriority = 2
                                isExpanded = false
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text(stringResource(R.string.cancel))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {
                            if (taskTitle.isNotEmpty()) {
                                val task = if (isEditMode) {
                                    existingTask!!.copy(
                                        title = taskTitle,
                                        description = taskDescription.ifEmpty { null },
                                        dueDate = taskDueDate,
                                        priority = taskPriority,
                                        syncStatus = "pending_update",
                                        lastModified = System.currentTimeMillis()
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
                                        createdAt = System.currentTimeMillis()
                                    )
                                }
                                onTaskCreated(task)
                                onDismiss()
                            }
                        },
                        enabled = taskTitle.isNotEmpty()
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
}
